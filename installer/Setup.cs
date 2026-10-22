using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using Microsoft.Win32;

// Per-user installation. Vault data is outside the application directory.
internal static class Setup
{
    private const string Product = "Passworder-9f2dc603-a471-4d12-a748-b44ca4cfe390";
    private const string Marker = ".passworder-install";
    private const string RegistryPath = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\Passworder";
    private static string root;
    private static string[] options;
    private static string version;
    private static Action<string> report = delegate { };

    [STAThread]
    private static int Main(string[] args)
    {
        options = args;
        bool removing = Has("--uninstall") || Array.IndexOf(Assembly.GetExecutingAssembly().GetManifestResourceNames(), "Passworder.Payload") < 0;
        root = Path.GetFullPath(Value("--root=", removing ? Path.GetDirectoryName(Application.ExecutablePath)
                : Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "Passworder")));
        Application.EnableVisualStyles(); Application.SetCompatibleTextRenderingDefault(false);
        if (removing && !Has("--worker"))
        {
            string folder = Path.Combine(Path.GetTempPath(), "Passworder-Uninstall-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(folder);
            string helper = Path.Combine(folder, "Uninstall.exe"); File.Copy(Application.ExecutablePath, helper);
            Process.Start(new ProcessStartInfo(helper, "--uninstall --worker --root=" + Quote(root) + (Has("--quiet") ? " --quiet" : "")) { UseShellExecute = false });
            return 0;
        }
        using (var mutex = new Mutex(false, @"Local\" + Product))
        {
            if (!mutex.WaitOne(0)) return 2;
            try
            {
                if (!removing) using (var reader = new StreamReader(Resource("Passworder.Build"))) version = reader.ReadLine();
                if (Has("--quiet")) return Run(removing);
                using (var window = new Form())
                {
                    window.Text = removing ? "Удаление Passworder" : "Установка Passworder " + version;
                    window.Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath);
                    window.ClientSize = new Size(480, 230); window.FormBorderStyle = FormBorderStyle.FixedDialog;
                    window.MaximizeBox = false; window.MinimizeBox = false; window.StartPosition = FormStartPosition.CenterScreen;
                    window.AutoScaleMode = AutoScaleMode.Dpi; window.Font = new Font("Segoe UI", 10);
                    window.BackColor = Color.FromArgb(21, 28, 38); window.ForeColor = Color.FromArgb(233, 239, 247);
                    var heading = new Label { Text = "Passworder", Font = new Font("Segoe UI", 22, FontStyle.Bold), Bounds = new Rectangle(28, 22, 410, 42) };
                    var status = new Label { Text = removing ? "Хранилище паролей останется на компьютере." : "Версия " + version, Bounds = new Rectangle(30, 78, 420, 28) };
                    var location = new Label { Text = root, ForeColor = Color.FromArgb(157, 174, 194), AutoEllipsis = true, Bounds = new Rectangle(30, 112, 420, 32) };
                    var button = new Button { Text = removing ? "Удалить" : "Установить", Bounds = new Rectangle(302, 170, 148, 40), FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(40, 95, 132) };
                    button.FlatAppearance.BorderSize = 0;
                    bool busy = false; int result = 1;
                    report = text => { if (!window.IsDisposed) window.Invoke(new Action(() => status.Text = text)); };
                    button.Click += async delegate
                    {
                        busy = true; button.Enabled = false;
                        result = await Task.Run(() => Run(removing)); busy = false;
                        if (result == 0) window.Close(); else { status.Text = "Операция не завершена."; button.Enabled = true; }
                    };
                    window.FormClosing += (sender, e) => { if (busy) e.Cancel = true; };
                    window.Controls.AddRange(new Control[] { heading, status, location, button }); window.AcceptButton = button;
                    Application.Run(window); return result;
                }
            }
            finally { mutex.ReleaseMutex(); }
        }
    }
    private static int Run(bool removing)
    {
        try { ValidateRoot(); if (removing) Remove(); else Install(); return 0; }
        catch (Exception error)
        {
            if (!Has("--quiet")) MessageBox.Show(error.Message, "Passworder", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return 1;
        }
    }
    private static void ValidateRoot()
    {
        string data = Path.GetFullPath(Path.Combine(Environment.GetEnvironmentVariable("LOCALAPPDATA") ?? Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Passworder"));
        if (root.TrimEnd(Path.DirectorySeparatorChar).Equals(Path.GetPathRoot(root).TrimEnd(Path.DirectorySeparatorChar), StringComparison.OrdinalIgnoreCase)
            || root.Equals(data, StringComparison.OrdinalIgnoreCase) || data.StartsWith(root + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase)
            || root.StartsWith(data + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase))
            throw new IOException("Недопустимая папка установки.");
        if (Directory.Exists(root) && Directory.GetFileSystemEntries(root).Length != 0 && !Owned(root))
            throw new IOException("Папка установки занята другими файлами: " + root);
        if (Directory.Exists(root) && (File.GetAttributes(root) & FileAttributes.ReparsePoint) != 0) throw new IOException("Папка установки не должна быть ссылкой.");
    }
    private static void Install()
    {
        string parent = Path.GetDirectoryName(root); Directory.CreateDirectory(parent);
        string stage = Path.Combine(parent, ".Passworder-stage-" + Guid.NewGuid().ToString("N"));
        string previous = root + ".previous"; bool moved = false, installed = false;
        Directory.CreateDirectory(stage); WriteMarker(stage);
        try
        {
            report("Подготовка файлов...");
            using (var payload = Resource("Passworder.Payload"))
            using (var zip = new ZipArchive(payload, ZipArchiveMode.Read))
            {
                var paths = new HashSet<string>(StringComparer.OrdinalIgnoreCase); long total = 0;
                foreach (var entry in zip.Entries)
                {
                    string destination = Path.GetFullPath(Path.Combine(stage, entry.FullName.Replace('/', Path.DirectorySeparatorChar)));
                    if (!destination.StartsWith(stage + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase)
                        || entry.FullName.Contains(":") || !paths.Add(destination) || (total += entry.Length) > 700L * 1024 * 1024 || paths.Count > 10000)
                        throw new IOException("Поврежден пакет установки.");
                    if (entry.FullName.EndsWith("/")) { Directory.CreateDirectory(destination); continue; }
                    Directory.CreateDirectory(Path.GetDirectoryName(destination));
                    using (var source = entry.Open()) using (var target = File.Create(destination)) source.CopyTo(target);
                }
            }
            string expected;
            using (var reader = new StreamReader(Resource("Passworder.Build"))) { reader.ReadLine(); expected = reader.ReadLine(); }
            using (var sha = SHA256.Create()) using (var jar = File.OpenRead(Path.Combine(stage, "app", "Passworder.jar")))
                if (BitConverter.ToString(sha.ComputeHash(jar)).Replace("-", "").ToLowerInvariant() != expected) throw new IOException("Поврежден файл приложения.");
            report("Завершение предыдущей версии..."); Prepare(stage);
            if (Directory.Exists(previous)) DeleteOwned(previous);
            if (Directory.Exists(root))
            {
                if (Directory.GetFileSystemEntries(root).Length == 0) Directory.Delete(root); else { Directory.Move(root, previous); moved = true; }
            }
            Directory.Move(stage, root); installed = true;
            if (!Has("--no-shortcuts")) Shortcuts(false);
            if (!Has("--no-registry")) Register();
            if (!Has("--no-launch")) Process.Start(new ProcessStartInfo(Path.Combine(root, "Passworder.exe")) { WorkingDirectory = root, UseShellExecute = false });
        }
        catch
        {
            if (installed && Directory.Exists(root)) DeleteOwned(root);
            if (moved && Directory.Exists(previous)) Directory.Move(previous, root);
            throw;
        }
        finally { if (Directory.Exists(stage)) DeleteOwned(stage); }
    }
    private static void Prepare(string image)
    {
        string arguments = "-cp " + Quote(Path.Combine(image, "app", "Passworder.jar")) + " svarcovv.dev.passworder.Passworder --prepare-update";
        using (var process = Process.Start(new ProcessStartInfo(Path.Combine(image, "runtime", "bin", "java.exe"), arguments) { WorkingDirectory = image, UseShellExecute = false, CreateNoWindow = true, RedirectStandardError = true }))
        {
            if (!process.WaitForExit(45000)) throw new IOException("Предыдущая версия еще завершает работу. Повторите установку.");
            if (process.ExitCode != 0) throw new IOException("Не удалось завершить предыдущий экземпляр Passworder. " + process.StandardError.ReadToEnd());
        }
        // The application releases its profile lock just before the native launcher exits.
        Thread.Sleep(300);
    }
    private static void Remove()
    {
        if (!Owned(root)) throw new IOException("Установка Passworder не найдена.");
        report("Завершение Passworder..."); Prepare(root);
        Shortcuts(true);
        using (var key = Registry.CurrentUser.OpenSubKey(RegistryPath))
            if (key != null && root.Equals(key.GetValue("InstallLocation") as string, StringComparison.OrdinalIgnoreCase)) Registry.CurrentUser.DeleteSubKeyTree(RegistryPath, false);
        DeleteOwned(root);
        if (Directory.Exists(root + ".previous")) DeleteOwned(root + ".previous");
    }
    private static void WriteMarker(string directory) { File.WriteAllText(Path.Combine(directory, Marker), Product + "\n" + version); }
    private static bool Owned(string directory) { string marker = Path.Combine(directory, Marker); return File.Exists(marker) && new FileInfo(marker).Length < 1024 && File.ReadAllText(marker).StartsWith(Product + "\n", StringComparison.Ordinal); }
    private static void DeleteOwned(string directory)
    {
        if (!Owned(directory)) throw new IOException("Папка не принадлежит установщику Passworder: " + directory);
        CheckLinks(directory); Directory.Delete(directory, true);
    }
    private static void CheckLinks(string directory)
    {
        if ((File.GetAttributes(directory) & FileAttributes.ReparsePoint) != 0) throw new IOException("В папке приложения обнаружена ссылка.");
        foreach (string child in Directory.GetDirectories(directory)) CheckLinks(child);
        foreach (string file in Directory.GetFiles(directory)) if ((File.GetAttributes(file) & FileAttributes.ReparsePoint) != 0) throw new IOException("В папке приложения обнаружена ссылка.");
    }
    private static void Shortcuts(bool remove)
    {
        Type type = Type.GetTypeFromProgID("WScript.Shell"); dynamic shell = Activator.CreateInstance(type);
        try
        {
            foreach (string folder in new[] { Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), Environment.GetFolderPath(Environment.SpecialFolder.Programs) })
            {
                string path = Path.Combine(folder, "Passworder.lnk");
                if (remove && !File.Exists(path)) continue;
                dynamic link = shell.CreateShortcut(path);
                try
                {
                    if (remove) { if (root.Equals(Path.GetDirectoryName((string)link.TargetPath), StringComparison.OrdinalIgnoreCase)) File.Delete(path); }
                    else { link.TargetPath = Path.Combine(root, "Passworder.exe"); link.WorkingDirectory = root; link.IconLocation = link.TargetPath + ",0"; link.Save(); }
                }
                finally { Marshal.FinalReleaseComObject(link); }
            }
        }
        finally { Marshal.FinalReleaseComObject(shell); }
    }
    private static void Register()
    {
        using (var key = Registry.CurrentUser.CreateSubKey(RegistryPath))
        {
            key.SetValue("DisplayName", "Passworder"); key.SetValue("DisplayVersion", version); key.SetValue("Publisher", "svarcovv");
            key.SetValue("InstallLocation", root); key.SetValue("DisplayIcon", Path.Combine(root, "Passworder.exe"));
            key.SetValue("UninstallString", Quote(Path.Combine(root, "Uninstall.exe")) + " --uninstall --root=" + Quote(root));
            key.SetValue("QuietUninstallString", Quote(Path.Combine(root, "Uninstall.exe")) + " --uninstall --quiet --root=" + Quote(root));
            key.SetValue("NoModify", 1); key.SetValue("NoRepair", 1);
        }
    }
    private static Stream Resource(string name) { return Assembly.GetExecutingAssembly().GetManifestResourceStream(name); }
    private static bool Has(string value) { return Array.IndexOf(options, value) >= 0; }
    private static string Value(string prefix, string fallback) { foreach (string arg in options) if (arg.StartsWith(prefix, StringComparison.Ordinal)) return arg.Substring(prefix.Length).Trim('"'); return fallback; }
    private static string Quote(string value) { return "\"" + value + "\""; }
}

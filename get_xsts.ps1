Add-Type @'
using System;
using System.Runtime.InteropServices;
using System.Text;
public class CR {
    [DllImport("advapi32.dll", CharSet = CharSet.Unicode)]
    static extern bool CredRead(string t, int type, int r, out IntPtr p);
    [DllImport("advapi32.dll")] static extern void CredFree(IntPtr p);
    public static string Get(string target) {
        IntPtr p;
        if (CredRead(target, 1, 0, out p)) {
            var c = (CRED)Marshal.PtrToStructure(p, typeof(CRED));
            byte[] b = new byte[c.S];
            Marshal.Copy(c.B, b, 0, c.S);
            CredFree(p);
            return Encoding.UTF8.GetString(b);
        }
        return null;
    }
    struct CRED { public int F; public int T; public string TN; public string C; public System.Runtime.InteropServices.ComTypes.FILETIME L; public int S; public IntPtr B; public int P; public int AC; public IntPtr A; public string TA; public string UN; }
}
'@
$t = [CR]::Get("XblGrts|1794566092|0007000083C72388|Production|RETAIL|Xtoken|rp://api.minecraftservices.com/||JWT|1")
if ($t) {
    Set-Content -Path "C:\Users\kaesh\Downloads\citadel\xsts_token.txt" -Value $t -Encoding UTF8
    Write-Host "XSTS token saved! Length: $($t.Length)"
} else {
    Write-Host "Token not found"
}

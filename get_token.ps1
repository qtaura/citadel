Add-Type @'
using System;
using System.Runtime.InteropServices;
using System.Text;
public class CredReader {
    [DllImport("advapi32.dll", CharSet = CharSet.Unicode)]
    static extern bool CredRead(string target, int type, int reserved, out IntPtr credential);
    [DllImport("advapi32.dll")]
    static extern void CredFree(IntPtr cred);
    public static string ReadAzureToken(string target) {
        IntPtr ptr;
        if (CredRead(target, 1, 0, out ptr)) {
            var cred = (CREDENTIAL)Marshal.PtrToStructure(ptr, typeof(CREDENTIAL));
            byte[] bytes = new byte[cred.CredentialBlobSize];
            Marshal.Copy(cred.CredentialBlob, bytes, 0, cred.CredentialBlobSize);
            CredFree(ptr);
            string raw = Encoding.UTF8.GetString(bytes);
            int jwtStart = raw.IndexOf("eyJ");
            return jwtStart >= 0 ? raw.Substring(jwtStart) : raw;
        }
        return null;
    }
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    struct CREDENTIAL {
        public int Flags; public int Type; public string TargetName; public string Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public int CredentialBlobSize; public IntPtr CredentialBlob; public int Persist;
        public int AttributeCount; public IntPtr Attributes; public string TargetAlias; public string UserName;
    }
}
'@
$token = [CredReader]::ReadAzureToken("MCLMS|6f97b65fdaf54fb8abb759a0c5fb36c0|AzureToken")
Set-Content -Path "$PSScriptRoot\token.txt" -Value $token -Encoding UTF8
Write-Host "Token saved to token.txt"

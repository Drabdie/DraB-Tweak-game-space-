// IShellService.aidl - executed inside the Shizuku server process (shell/root user)
package com.drabdie.tweak;

interface IShellService {
    // Returns {exitCode, stdout, stderr}
    String[] exec(String cmd);
}

// IShellService.aidl — privileged command executor running inside the Shizuku server process.
package com.drabdie.tweak;

interface IShellService {
    // Returns [0]=exit code, [1]=combined stdout+stderr (truncated).
    String[] exec(String cmd);
}

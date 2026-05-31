#!/usr/bin/env python3
"""
Neotune Unified API Server Launcher
This script consolidates IP detection, yt-dlp updates, port cleanup, and FastAPI startup.
Includes Windows-safe console rendering to prevent UnicodeEncodeErrors.
"""
import os
import sys
import socket
import subprocess
import platform
import re
import signal
import time

def handle_signal(sig, frame):
    """Graceful shutdown handler"""
    print("\n[INFO] Shutting down Neotune API Server...")
    sys.exit(0)

def get_local_ip():
    """Detect the computer's primary local network IP address"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception:
        return None

def print_welcome_banner(ip_address):
    """Prints a gorgeous, clean setup guide on launch"""
    border = "=========================================================="
    print("\n" + border)
    print("        NEOTUNE API SERVER UNIFIED LAUNCHER")
    print(border)
    
    if ip_address:
        print(f"\n[Step 1/4] Network Discovery")
        print(f"    - Local network IP detected: \033[92m{ip_address}\033[0m")
        print(f"    - Backend Base URL:         \033[96mhttp://{ip_address}:8000\033[0m")
        print("\n[MOBILE SETUP] Easy App Connection:")
        print("    1. Open the Neotune App on your phone.")
        print("    2. Navigate to Settings -> Connection.")
        print(f"    3. Enter the Server IP: \033[93m{ip_address}\033[0m and tap Apply!")
        print("\n[TIP] Quick Check:")
        print("    1. Ensure both your computer and phone are on the SAME Wi-Fi network.")
        print("    2. Allow inbound connections on port 8000 in your firewall if needed.")
    else:
        print("\n[Step 1/4] Network Discovery")
        print("    Could not automatically detect your local network IP.")
        print("    Running server on default host: http://localhost:8000")
        print("    You may need to manually search for your local IP using:")
        print("    - Windows: ipconfig  |  - Mac/Linux: ifconfig")
        
    print(border + "\n")

def update_ytdlp():
    """Checks and updates the critical yt-dlp dependency"""
    print("[Step 2/4] Checking and upgrading yt-dlp audio extractor...")
    try:
        # 1. Pip package update
        subprocess.run([sys.executable, "-m", "pip", "install", "--upgrade", "yt-dlp"], 
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        
        # 2. Try the yt-dlp self-update mechanism directly
        try:
            subprocess.run([sys.executable, "-m", "yt_dlp", "-U"], 
                           check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception:
            pass # Self-update can fail depending on permission/installation source, not critical
            
        print("[SUCCESS] yt-dlp successfully updated and verified!")
        return True
    except Exception as e:
        print(f"[WARN] Failed to update yt-dlp: {e}")
        print("    Continuing with the currently installed version...")
        return False

def clear_port_if_needed(port=8000):
    """Scans and clears zombie processes on the server port"""
    print(f"\n[Step 3/4] Scanning network port {port}...")
    
    # Try connecting to see if the port is busy
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        if s.connect_ex(('127.0.0.1', port)) != 0:
            print(f"[SUCCESS] Port {port} is free and ready!")
            return True

    print(f"[WARN] Port {port} is currently in use!")
    pid = None
    os_name = platform.system()
    
    try:
        # Identify blocking PID
        if os_name == "Windows":
            cmd = f'netstat -ano | findstr ":{port}"'
            result = subprocess.check_output(cmd, shell=True, text=True, stderr=subprocess.DEVNULL)
            match = re.search(r'LISTENING\s+(\d+)', result)
            if match:
                pid = match.group(1)
        elif os_name in ["Linux", "Darwin"]:
            cmd = f'lsof -ti tcp:{port}'
            result = subprocess.check_output(cmd, shell=True, text=True, stderr=subprocess.DEVNULL)
            pid = result.strip().split('\n')[0]

        if pid:
            print(f"[CONFLICT] A zombie/existing server (PID {pid}) is blocking port {port}.")
            choice = input(f"    Would you like to terminate process {pid} to clear the port? (y/n): ").strip().lower()
            if choice == 'y':
                print(f"    Terminating process {pid}...")
                if os_name == "Windows":
                    subprocess.run(f"taskkill /PID {pid} /F", shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                else:
                    subprocess.run(f"kill -9 {pid}", shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                print("    [SUCCESS] Port cleared successfully!")
                time.sleep(1.5)
                return True
            else:
                print("    [WARN] Aborted. Server launch might fail if the port remains locked.")
                return False
        else:
            print("    Could not automatically identify the process. Attempting server spinup...")
            return True
            
    except Exception as e:
        print(f"    [WARN] Could not search or clear port: {e}")
        return True

def main():
    # Setup signal interception
    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)
    
    # 1. Discover local network IP
    local_ip = get_local_ip()
    print_welcome_banner(local_ip)
    
    # 2. Update yt-dlp extractor
    update_ytdlp()
    
    # 3. Clear port conflicts
    clear_port_if_needed(8000)
    
    # 4. Launch FastAPI Server
    print("\n[Step 4/4] Launching Neotune API FastAPI server...")
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        os.chdir(script_dir)
        
        # Run main.py as a subprocess and stream console output
        server_process = subprocess.Popen([sys.executable, "main.py"])
        
        # Monitor the process state
        while server_process.poll() is None:
            time.sleep(1)
            
        if server_process.returncode != 0:
            print(f"\n[ERROR] Server exited with error code {server_process.returncode}")
            return server_process.returncode
            
    except KeyboardInterrupt:
        print("\n[INFO] Received termination signal. Shutting down server...")
        if 'server_process' in locals() and server_process.poll() is None:
            server_process.terminate()
            try:
                server_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server_process.kill()
    except Exception as e:
        print(f"\n[ERROR] Failed to run API server: {e}")
        return 1
        
    return 0

if __name__ == "__main__":
    sys.exit(main())
/*
 * A Java runtime that comes with the core instead of being installed.
 *
 * FreeJ2ME's Java app is started with "java" (javaw on Windows) from PATH, so
 * the core works only where a Java runtime is installed and on PATH - which a
 * frontend in a sandbox, a handheld or a portable setup often does not have.
 *
 * "ant build-runtime" makes a runtime for it with jlink: java.base and
 * java.desktop, the two modules FreeJ2ME-Plus uses, ~50 MB instead of a whole
 * JRE, as freej2me_plus_runtime_<platform>.zip. The core uses the one for its
 * own platform only, so a system directory shared between machines of
 * different kinds (synced, or on a card) can hold one runtime for each:
 *
 *   system/freej2me_plus_runtime_<platform>.zip    as downloaded; unpacked
 *                                                  once, again when it changes
 *   system/freej2me_plus_runtime/<platform>/       the runtime, unpacked by the
 *                                                  core or by hand
 *
 * The archive is a .tar.gz, or a .zip for Windows (see build.xml), so that the
 * system's tar unpacks it: GNU tar on Linux, bsdtar on macOS and on Windows 10
 * and later.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#if defined(_WIN32)
#include <windows.h>
#include <direct.h>
#else
#include <dirent.h>
#include <unistd.h>
#include <sys/wait.h>
#endif

#include "bundled_jre.h"

#if defined(_WIN32)
#define SLASH "\\"
#define JAVA_EXE "javaw.exe"
#else
#define SLASH "/"
#define JAVA_EXE "java"
#endif

/* The platform names of "ant build-runtime -Dplatform=..." */
#if defined(_WIN32)
#if defined(_M_ARM64) || defined(__aarch64__)
#define RUNTIME_PLATFORM "windows-arm64"
#elif defined(_WIN64)
#define RUNTIME_PLATFORM "windows-x64"
#else
#define RUNTIME_PLATFORM "windows-x86"
#endif
#define RUNTIME_ARCHIVE_EXT ".zip"
#elif defined(__APPLE__)
#if defined(__aarch64__) || defined(__arm64__)
#define RUNTIME_PLATFORM "macos-arm64"
#else
#define RUNTIME_PLATFORM "macos-x64"
#endif
#define RUNTIME_ARCHIVE_EXT ".tar.gz"
#else
#if defined(__aarch64__)
#define RUNTIME_PLATFORM "linux-arm64"
#elif defined(__x86_64__)
#define RUNTIME_PLATFORM "linux-x64"
#elif defined(__i386__)
#define RUNTIME_PLATFORM "linux-x86"
#elif defined(__arm__)
#define RUNTIME_PLATFORM "linux-arm"
#else
#define RUNTIME_PLATFORM "linux-other"
#endif
#define RUNTIME_ARCHIVE_EXT ".tar.gz"
#endif

static bool file_exists(const char *path)
{
	struct stat st;
	return stat(path, &st) == 0;
}

static bool is_dir(const char *path)
{
	struct stat st;
	return stat(path, &st) == 0 && (st.st_mode & S_IFDIR);
}

static void make_dirs(const char *path)
{
	char tmp[BUNDLED_JRE_PATH_MAX];
	size_t i, len;

	snprintf(tmp, sizeof(tmp), "%s", path);
	len = strlen(tmp);
	for (i = 1; i <= len; i++)
	{
		if (tmp[i] == '/' || tmp[i] == '\\' || tmp[i] == '\0')
		{
			char c = tmp[i];
			tmp[i] = '\0';
			if (!is_dir(tmp))
			{
#if defined(_WIN32)
				_mkdir(tmp);
#else
				mkdir(tmp, 0755);
#endif
			}
			tmp[i] = c;
		}
	}
}

/* Deletes a directory and everything in it; a previous unpack, or one that
 * was interrupted. Symbolic links are removed, not followed. */
static void remove_tree(const char *path)
{
#if defined(_WIN32)
	char pattern[BUNDLED_JRE_PATH_MAX];
	WIN32_FIND_DATAA fd;
	HANDLE h;

	snprintf(pattern, sizeof(pattern), "%s\\*", path);
	h = FindFirstFileA(pattern, &fd);
	if (h != INVALID_HANDLE_VALUE)
	{
		do
		{
			char child[BUNDLED_JRE_PATH_MAX];
			if (!strcmp(fd.cFileName, ".") || !strcmp(fd.cFileName, ".."))
				continue;
			snprintf(child, sizeof(child), "%s\\%s", path, fd.cFileName);
			if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)
				remove_tree(child);
			else
			{
				SetFileAttributesA(child, FILE_ATTRIBUTE_NORMAL);
				DeleteFileA(child);
			}
		} while (FindNextFileA(h, &fd));
		FindClose(h);
	}
	RemoveDirectoryA(path);
#else
	DIR *dir = opendir(path);
	struct dirent *entry;

	if (dir)
	{
		while ((entry = readdir(dir)) != NULL)
		{
			char child[BUNDLED_JRE_PATH_MAX];
			struct stat st;
			if (!strcmp(entry->d_name, ".") || !strcmp(entry->d_name, ".."))
				continue;
			snprintf(child, sizeof(child), "%s/%s", path, entry->d_name);
			if (lstat(child, &st) == 0 && S_ISDIR(st.st_mode))
				remove_tree(child);
			else
				unlink(child);
		}
		closedir(dir);
	}
	rmdir(path);
#endif
}

/* Runs the system's tar on the archive, into dest; true when it succeeded. */
static bool run_tar(const char *archive, const char *dest)
{
#if defined(_WIN32)
	char cmd[BUNDLED_JRE_PATH_MAX * 2 + 32];
	STARTUPINFOA si;
	PROCESS_INFORMATION pi;
	DWORD code = 1;

	snprintf(cmd, sizeof(cmd), "tar.exe -xf \"%s\" -C \"%s\"", archive, dest);
	ZeroMemory(&si, sizeof(si));
	si.cb = sizeof(si);
	ZeroMemory(&pi, sizeof(pi));
	if (!CreateProcessA(NULL, cmd, NULL, NULL, FALSE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi))
		return false;
	WaitForSingleObject(pi.hProcess, INFINITE);
	GetExitCodeProcess(pi.hProcess, &code);
	CloseHandle(pi.hProcess);
	CloseHandle(pi.hThread);
	return code == 0;
#else
	int status = 0;
	pid_t pid = fork();

	if (pid == 0)
	{
		execlp("tar", "tar", "-xf", archive, "-C", dest, (char*)NULL);
		_exit(127);
	}
	if (pid < 0 || waitpid(pid, &status, 0) < 0)
		return false;
	return WIFEXITED(status) && WEXITSTATUS(status) == 0;
#endif
}

bool bundled_jre_find(const char *systemDir,
	char *java, size_t java_len, retro_environment_t environ_cb, retro_log_printf_t log)
{
	char archive[BUNDLED_JRE_PATH_MAX], parentDir[BUNDLED_JRE_PATH_MAX], runtimeDir[BUNDLED_JRE_PATH_MAX];
	char tmpDir[BUNDLED_JRE_PATH_MAX], unpacked[BUNDLED_JRE_PATH_MAX];
	char stampPath[BUNDLED_JRE_PATH_MAX], stamp[BUNDLED_JRE_PATH_MAX], current[BUNDLED_JRE_PATH_MAX];
	struct stat st;
	FILE *f;

	snprintf(archive, sizeof(archive), "%s%sfreej2me_plus_runtime_" RUNTIME_PLATFORM RUNTIME_ARCHIVE_EXT, systemDir, SLASH);
	snprintf(parentDir, sizeof(parentDir), "%s%sfreej2me_plus_runtime", systemDir, SLASH);
	snprintf(runtimeDir, sizeof(runtimeDir), "%s%s" RUNTIME_PLATFORM, parentDir, SLASH);
	snprintf(java, java_len, "%s%sbin%s" JAVA_EXE, runtimeDir, SLASH, SLASH);
	snprintf(stampPath, sizeof(stampPath), "%s%s.source", runtimeDir, SLASH);

	/* No archive: a runtime unpacked by hand, if there is one */
	if (stat(archive, &st) != 0 || (st.st_mode & S_IFDIR))
	{
		if (!file_exists(java))
			return false;
		log(RETRO_LOG_INFO, "Using the Java runtime in %s\n", runtimeDir);
		return true;
	}

	/* Unpacked from this same archive already */
	snprintf(stamp, sizeof(stamp), "%lld %lld", (long long)st.st_size, (long long)st.st_mtime);
	current[0] = '\0';
	if ((f = fopen(stampPath, "r")) != NULL)
	{
		if (!fgets(current, sizeof(current), f))
			current[0] = '\0';
		fclose(f);
	}
	if (!strcmp(current, stamp) && file_exists(java))
	{
		log(RETRO_LOG_INFO, "Using the Java runtime in %s\n", runtimeDir);
		return true;
	}

	log(RETRO_LOG_INFO, "Unpacking the Java runtime from %s into %s\n", archive, runtimeDir);
	if (environ_cb)
	{
		struct retro_message_ext msg = { "Unpacking the Java runtime...", 3000, 1, RETRO_LOG_INFO,
			RETRO_MESSAGE_TARGET_ALL, RETRO_MESSAGE_TYPE_NOTIFICATION, -1 };
		environ_cb(RETRO_ENVIRONMENT_SET_MESSAGE_EXT, &msg);
	}

	/* Into a directory of its own first, so that an interrupted unpack never
	 * passes for a complete one. The archive holds
	 * freej2me_plus_runtime/<platform>/. */
	snprintf(tmpDir, sizeof(tmpDir), "%s%sfreej2me_plus_runtime.tmp", systemDir, SLASH);
	remove_tree(tmpDir);
	make_dirs(tmpDir);
	if (!run_tar(archive, tmpDir))
	{
		log(RETRO_LOG_ERROR, "Could not unpack %s (is tar available?)\n", archive);
		remove_tree(tmpDir);
		return false;
	}
	snprintf(unpacked, sizeof(unpacked), "%s%sfreej2me_plus_runtime%s" RUNTIME_PLATFORM, tmpDir, SLASH, SLASH);
	snprintf(current, sizeof(current), "%s%sbin%s" JAVA_EXE, unpacked, SLASH, SLASH);
	if (!file_exists(current))
	{
		log(RETRO_LOG_ERROR, "%s holds no freej2me_plus_runtime/" RUNTIME_PLATFORM "/bin/" JAVA_EXE "\n", archive);
		remove_tree(tmpDir);
		return false;
	}
	remove_tree(runtimeDir);
	make_dirs(parentDir);
	if (rename(unpacked, runtimeDir) != 0)
	{
		log(RETRO_LOG_ERROR, "Could not move the unpacked Java runtime to %s\n", runtimeDir);
		remove_tree(tmpDir);
		return false;
	}
	remove_tree(tmpDir);
	if ((f = fopen(stampPath, "w")) != NULL)
	{
		fputs(stamp, f);
		fclose(f);
	}
	if (!file_exists(java))
		return false;
	log(RETRO_LOG_INFO, "Using the Java runtime in %s\n", runtimeDir);
	return true;
}

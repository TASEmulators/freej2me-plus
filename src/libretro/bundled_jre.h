#ifndef BUNDLED_JRE_H
#define BUNDLED_JRE_H

#include <stdbool.h>
#include <stddef.h>

#include "libretro.h"

#define BUNDLED_JRE_PATH_MAX 4096

/* Writes the java executable (javaw.exe on Windows) of the runtime "ant
 * build-runtime" makes for this platform to java: from
 * systemDir/freej2me_plus_runtime/<platform>/, which
 * systemDir/freej2me_plus_runtime_<platform>.tar.gz (.zip on Windows) is
 * unpacked into first when it is there and has not been yet. Returns false
 * when there is neither; the java on PATH is the one to start then. */
bool bundled_jre_find(const char *systemDir,
	char *java, size_t java_len, retro_environment_t environ_cb, retro_log_printf_t log);

#endif

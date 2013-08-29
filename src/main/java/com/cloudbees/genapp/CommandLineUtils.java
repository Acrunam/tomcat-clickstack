/*
 * Copyright 2010-2013, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudbees.genapp;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class CommandLineUtils {
    public static String getOption(@Nonnull String name, @Nonnull String[] args) throws NoSuchElementException {
        return getOption(name, args, true, true);
    }

    public static String getOption(@Nonnull String name, @Nonnull String[] args, boolean defaultToSystemProperty, boolean defaultToEnvironmentVariable) throws NoSuchElementException {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (Strings2.beginWith(arg, "--")) {
                arg = arg.substring(1);
            }

            if (arg.equals("-" + name)) {
                if ((i + 1) < args.length) {
                    return args[i + 1];
                }
            } else if (Strings2.beginWith(arg, "-" + name + "=")) {
                return Strings2.substringAfterFirst(arg, '=');
            }
        }
        if (defaultToSystemProperty && (System.getProperty(name) != null)) {
            return System.getProperty(name);
        }
        if (defaultToEnvironmentVariable && System.getenv(name) != null) {
            return System.getenv(name);
        }

        throw new NoSuchElementException("Argument '" + name + "' not found in " + Arrays.asList(args));
    }
}

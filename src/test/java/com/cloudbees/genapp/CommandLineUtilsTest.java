/*
 * Copyright 2010-2013, the original author or authors
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

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static com.cloudbees.genapp.CommandLineUtils.*;

import org.junit.Test;

import java.util.Properties;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class CommandLineUtilsTest {

    @Test
    public void get_option_from_args() {
        String[] args = new String[]{
                "-app_dir=/tmp/app_dir",
                "--app_port=8080",
                "-plugin_dir", "/tmp/plugin_dir",
                "--pkg_dir", "/tmp/pkg_dir"};

        assertThat(getOption("app_dir", args), is("/tmp/app_dir"));
        assertThat(getOption("app_port", args), is("8080"));
        assertThat(getOption("plugin_dir", args), is("/tmp/plugin_dir"));
        assertThat(getOption("pkg_dir", args), is("/tmp/pkg_dir"));
    }

    @Test
    public void get_option_from_system_properties() {
        String[] args = new String[]{
                "-app_dir=/tmp/app_dir",
                "--app_port=8080",
                "-plugin_dir", "/tmp/plugin_dir",
                "--pkg_dir", "/tmp/pkg_dir"};

        Properties props = new Properties();
        props.setProperty("genapp_dir", "/tmp/genapp_dir");
        System.setProperties(props);
        try {
            assertThat(getOption("genapp_dir", args), is("/tmp/genapp_dir"));
        } finally {
            for (String name : props.stringPropertyNames()) {
                System.getProperties().remove(name);
            }
        }
    }

    @Test
    public void get_option_mix_args_and_system_properties() {
        String[] args = new String[]{
                "-app_dir=/tmp/app_dir",
                "--app_port=8080",
                "-plugin_dir", "/tmp/plugin_dir",
                "--pkg_dir", "/tmp/pkg_dir"};

        Properties props = new Properties();
        props.setProperty("genapp_dir", "/tmp/genapp_dir");
        System.setProperties(props);
        try {
            assertThat(getOption("app_dir", args), is("/tmp/app_dir"));
            assertThat(getOption("app_port", args), is("8080"));
            assertThat(getOption("plugin_dir", args), is("/tmp/plugin_dir"));
            assertThat(getOption("pkg_dir", args), is("/tmp/pkg_dir"));

            assertThat(getOption("genapp_dir", args), is("/tmp/genapp_dir"));
        } finally {
            for (String name : props.stringPropertyNames()) {
                System.getProperties().remove(name);
            }
        }
    }
}

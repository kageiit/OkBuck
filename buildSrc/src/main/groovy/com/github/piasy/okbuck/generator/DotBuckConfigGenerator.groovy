/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.generator

import com.github.piasy.okbuck.OkBuckExtension
import com.github.piasy.okbuck.config.DotBuckConfigFile
import com.github.piasy.okbuck.model.ProjectType
import com.github.piasy.okbuck.model.Target
import com.github.piasy.okbuck.util.ProjectUtil
import org.gradle.api.Project

/**
 * Created by Piasy{github.com/Piasy} on 15/10/6.
 *
 * used to generate .buckconfig file content.
 */
final class DotBuckConfigGenerator {

    private DotBuckConfigGenerator() {}

    /**
     * generate {@link DotBuckConfigFile}
     */
    static DotBuckConfigFile generate(OkBuckExtension okbuck) {
        Map<String, String> aliases = [:]
        okbuck.buckProjects.findAll { Project project ->
            ProjectUtil.getType(project) == ProjectType.ANDROID_APP
        }.each { Project project ->
            ProjectUtil.getTargets(project).each { String name, Target target ->
                aliases.put("${target.identifier}${name.capitalize()}", "//${target.path}:bin_${name}")
            }
        }

        return new DotBuckConfigFile(aliases, okbuck.buildToolVersion, okbuck.target, [".git", "**/.svn"])
    }
}

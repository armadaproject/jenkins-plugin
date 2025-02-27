/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armadaproject.jenkins.plugin;

import static org.junit.Assert.*;

import io.armadaproject.jenkins.plugin.volumes.ConfigMapVolume;
import org.junit.Test;

public class ConfigMapVolumeTest {

    @Test
    public void testNullSubPathValue() {
        ConfigMapVolume configMapVolume = new ConfigMapVolume("oneMountPath", "Myvolume", false);
        assertNull(configMapVolume.getSubPath());
    }

    @Test
    public void testValidSubPathValue() {
        ConfigMapVolume configMapVolume = new ConfigMapVolume("oneMountPath", "Myvolume", false);
        configMapVolume.setSubPath("miSubpath");
        assertEquals(configMapVolume.getSubPath(), "miSubpath");
    }
}

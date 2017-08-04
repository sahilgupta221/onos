/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.pcepio.types;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test case for TEDefaultMetricSubTlv.
 */
public class TEDefaultMetricSubTlvTest {

    private final TEDefaultMetricSubTlv tlv1 = TEDefaultMetricSubTlv.of(1);
    private final TEDefaultMetricSubTlv tlv2 = TEDefaultMetricSubTlv.of(1);
    private final TEDefaultMetricSubTlv tlv3 = TEDefaultMetricSubTlv.of(2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }
}

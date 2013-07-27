/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.common.registry;

import org.alfresco.util.VersionNumber;
import org.junit.Assert;
import org.junit.Test;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.FallsInVersionRangeCondition;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class FallsInVersionRangeConditionTest
{

    @Test
    public void testBothEndsExclusive()
    {
        final FallsInVersionRangeCondition condition = new FallsInVersionRangeCondition(new VersionNumber("1.2.3"), true,
                new VersionNumber("1.3"), true, true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();
        script.setForCommunity(true);

        script.setVersion(new VersionNumber("1.2.4"));
        Assert.assertTrue("FallsInVersionRangeExclusive 1.2.3-1.3 does not match 1.2.4", condition.matches(script));
        script.setVersion(new VersionNumber("1.2.3.1"));
        Assert.assertTrue("FallsInVersionRangeExclusive 1.2.3-1.3 does not match 1.2.3.1", condition.matches(script));
        script.setVersion(new VersionNumber("1.2.9"));
        Assert.assertTrue("FallsInVersionRangeExclusive 1.2.3-1.3 does not match 1.2.9", condition.matches(script));

        script.setVersion(new VersionNumber("1.2.3"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3 does match 1.2.3", condition.matches(script));
        script.setVersion(new VersionNumber("1.2.3.0"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3 does match 1.2.3.0", condition.matches(script));
        script.setVersion(new VersionNumber("1.3"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3 does match 1.3", condition.matches(script));
        script.setVersion(new VersionNumber("1.3.0"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3 does match 1.3.0", condition.matches(script));
    }

    @Test
    public void testOpenEnded()
    {
        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();
        script.setForCommunity(true);
        {
            final FallsInVersionRangeCondition condition = new FallsInVersionRangeCondition(null, false, new VersionNumber("1.3"), true,
                    true);

            script.setVersion(new VersionNumber("0"));
            Assert.assertTrue("FallsInVersionRangeOpenLowerEnd -1.3 does not match 0", condition.matches(script));
            script.setVersion(new VersionNumber("-20"));
            Assert.assertTrue("FallsInVersionRangeOpenLowerEnd -1.3 does not match -20", condition.matches(script));
            script.setVersion(new VersionNumber("1.2.9"));
            Assert.assertTrue("FallsInVersionRangeOpenLowerEnd -1.3 does not match 1.2.9", condition.matches(script));
        }

        {
            final FallsInVersionRangeCondition condition = new FallsInVersionRangeCondition(new VersionNumber("1.3"), true, null, false,
                    true);

            script.setVersion(new VersionNumber("999"));
            Assert.assertTrue("FallsInVersionRangeOpenUpperEnd 1.3- does not match 999", condition.matches(script));
            script.setVersion(new VersionNumber("1.3.1"));
            Assert.assertTrue("FallsInVersionRangeOpenUpperEnd 1.3- does not match 1.3.1", condition.matches(script));
            script.setVersion(new VersionNumber("1.4"));
            Assert.assertTrue("FallsInVersionRangeOpenUpperEnd 1.3- does not match 1.4", condition.matches(script));
        }
    }
}

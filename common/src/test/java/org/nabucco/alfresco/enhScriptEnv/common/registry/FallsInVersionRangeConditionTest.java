package org.nabucco.alfresco.enhScriptEnv.common.registry;

import org.alfresco.util.VersionNumber;
import org.junit.Assert;
import org.junit.Test;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.AppliesForVersionCondition;
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
                new VersionNumber("1.3"), true);

        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();

        script.setVersion(new VersionNumber("1.2.4"));
        Assert.assertTrue("FallsInVersionRangeExclusive 1.2.3-1.3 does not match 1.2.4", condition.matches(script));
        script.setVersion(new VersionNumber("1.2.3.1"));
        Assert.assertTrue("FallsInVersionRangeExclusive 1.2.3-1.3 does not match 1.2.3.1", condition.matches(script));
        script.setVersion(new VersionNumber("1.2.9"));
        Assert.assertTrue("FallsInVersionRangeExclusive 1.2.3-1.3  does not match 1.2.9", condition.matches(script));

        script.setVersion(new VersionNumber("1.2.3"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3 does match 1.2.3", condition.matches(script));
        script.setVersion(new VersionNumber("1.2.3.0"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3  does match 1.2.3.0", condition.matches(script));
        script.setVersion(new VersionNumber("1.3"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3  does match 1.3", condition.matches(script));
        script.setVersion(new VersionNumber("1.3.0"));
        Assert.assertFalse("FallsInVersionRangeExclusive 1.2.3-1.3 does match 1.3.0", condition.matches(script));
    }

    @Test
    public void testOpenEnded()
    {
        final DummyVersionRegisterableScript script = new DummyVersionRegisterableScript();
        {
            final FallsInVersionRangeCondition condition = new FallsInVersionRangeCondition(null, false, new VersionNumber("1.3"), true);

            script.setVersion(new VersionNumber("0"));
            Assert.assertTrue("FallsInVersionRangeOpenLowerEnd -1.3 does not match 0", condition.matches(script));
            script.setVersion(new VersionNumber("-20"));
            Assert.assertTrue("FallsInVersionRangeOpenLowerEnd -1.3 does not match -20", condition.matches(script));
            script.setVersion(new VersionNumber("1.2.9"));
            Assert.assertTrue("FallsInVersionRangeOpenLowerEnd -1.3  does not match 1.2.9", condition.matches(script));
        }

        {
            final FallsInVersionRangeCondition condition = new FallsInVersionRangeCondition(new VersionNumber("1.3"), true, null, false);
            script.setVersion(new VersionNumber("999"));
            Assert.assertTrue("FallsInVersionRangeOpenUpperEnd 1.3- does not match 999", condition.matches(script));
            script.setVersion(new VersionNumber("1.3.1"));
            Assert.assertTrue("FallsInVersionRangeOpenUpperEnd 1.3- does not match 1.3.1", condition.matches(script));
            script.setVersion(new VersionNumber("1.4"));
            Assert.assertTrue("FallsInVersionRangeOpenUpperEnd 1.3-  does not match 1.4", condition.matches(script));
        }
    }
}

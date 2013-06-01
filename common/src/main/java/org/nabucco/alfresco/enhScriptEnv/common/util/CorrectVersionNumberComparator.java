package org.nabucco.alfresco.enhScriptEnv.common.util;

import java.util.Comparator;

import org.alfresco.util.VersionNumber;

/**
 * A custom version number comparator that takes different lengths of version numbers into account, but only considers additional parts that
 * do add information to the overall number (e.g. a 1.2.0 is no different than 1.2, but a 1.2.0.1 is very much different than 1.2).
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class CorrectVersionNumberComparator implements Comparator<VersionNumber>
{
    protected static final CorrectVersionNumberComparator INSTANCE = new CorrectVersionNumberComparator();

    public static int compareVersions(final VersionNumber o1, final VersionNumber o2)
    {
        return INSTANCE.compare(o1, o2);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int compare(final VersionNumber o1, final VersionNumber o2)
    {
        final int result;

        if (o1 != null && o2 == null)
        {
            result = 1;
        }
        else if (o1 == null && o2 != null)
        {
            result = -1;
        }
        else if (o1 == o2)
        {
            result = 0;
        }
        else
        {
            // can't be null, but compiler can't determine that we've eliminated the possibility
            @SuppressWarnings("null")
            final int[] parts1 = o1.getParts();
            final int[] parts2 = o2.getParts();

            int iterResult = 0;
            for (int idx = 0; idx < parts1.length && idx < parts2.length; idx++)
            {
                if (parts1[idx] > parts2[idx])
                {
                    iterResult = 1;
                    break;
                }
                else if (parts1[idx] < parts2[idx])
                {
                    iterResult = -1;
                    break;
                }
            }

            if (iterResult == 0 && parts1.length != parts2.length)
            {
                if (parts1.length > parts2.length)
                {
                    // find non-zero part
                    for (int idx = parts2.length; idx < parts1.length; idx++)
                    {
                        if (parts1[idx] > 0)
                        {
                            iterResult = 1;
                            break;
                        }
                    }
                }
                else
                {
                    // find non-zero part
                    for (int idx = parts1.length; idx < parts2.length; idx++)
                    {
                        if (parts2[idx] > 0)
                        {
                            iterResult = -1;
                            break;
                        }
                    }
                }
            }

            result = iterResult;
        }

        return result;
    }

}

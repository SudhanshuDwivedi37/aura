/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.util.test;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;

import org.auraframework.test.UnitTestCase;
import org.auraframework.util.test.perf.PerfDiffUtils;
import org.auraframework.util.test.perf.PerfGoldFilesUtil;
import org.auraframework.util.test.perf.data.PerfMetrics;

public final class GoldFileUtils {

    private static final Logger LOG = Logger.getLogger(GoldFileUtils.class.getSimpleName());

    private static final boolean SKIP_GOLD_FILE_UPDATE = System.getProperty("skipGoldFileUpdate") != null;

    public void assertTextDiff(UnitTestCase test, String resultsBaseFilename, String testResults) throws Exception {
        TextDiffUtils diff = new TextDiffUtils(test, resultsBaseFilename);
        assertDiffInternal(testResults, diff);
    }

    public void assertJsonDiff(UnitTestCase test, String resultsBaseFilename, String testResults) throws Exception {
        TextDiffUtils diff = new JsonDiffUtils(test, resultsBaseFilename);
        assertDiffInternal(testResults, diff);
    }

    public void assertPerfDiff(UnitTestCase test, String resultsBaseFilename, PerfMetrics actual) throws Exception {
        PerfDiffUtils diff = new PerfDiffUtils(test, resultsBaseFilename);
        assertDiffInternal(actual, diff);
    }

    private <T> void assertDiffInternal(T testResults, DiffUtils<T> diff) throws Exception {
        URL url = diff.getUrl();
        Throwable exceptionFound = null;
        String message = null;

        try {
            diff.assertDiff(testResults, null);
        } catch (FileNotFoundException e) {
            exceptionFound = e;
            message = String.format("Created missing gold file, review new gold file before committing: %s", url);
        } catch (Throwable t) {
            exceptionFound = t;
            message = "Gold file differences found";
            if (!SKIP_GOLD_FILE_UPDATE) {
                message += String.format(", review updated gold file before committing: %s", url);
            }
            message += "\nDifferences:\n" + t.getMessage();
        }

        if (exceptionFound != null) {
            if (exceptionFound instanceof FileNotFoundException || !SKIP_GOLD_FILE_UPDATE) {
                LOG.info("writing log file: " + url);
                diff.writeGoldFile(testResults);
            }
            if (testResults instanceof PerfMetrics) {
                LOG.info("new gold file contents:\n"
                        + PerfGoldFilesUtil.toGoldFileText((PerfMetrics) testResults, diff.getTest()
                                .storeDetailsInGoldFile()));
            }

            // add info about creating/updating log file in the message
            Error error = new AssertionFailedError(message);
            error.setStackTrace(exceptionFound.getStackTrace());
            throw error;
        }
    }
}

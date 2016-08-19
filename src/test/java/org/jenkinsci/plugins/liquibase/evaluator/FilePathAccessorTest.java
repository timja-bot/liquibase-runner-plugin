package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

@RunWith(MockitoJUnit44Runner.class)
public class FilePathAccessorTest {

    private static final String CHILD_DIR = "childDir";
    @Mock
    AbstractBuild build;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Test
    public void should_list_files() throws IOException, InterruptedException {
        temporaryFolder.create();
        File childDir = temporaryFolder.newFolder(CHILD_DIR);
        childDir.mkdirs();

        File childFile = new File(childDir, "child.xml");
        String childFilepath = childFile.getAbsolutePath();
        FileUtils.touch(childFile);

        FilePath filePath = new FilePath(temporaryFolder.getRoot());

        FilePathAccessor filePathAccessor = new FilePathAccessor(build);

        Set<String> result =
                filePathAccessor.list( filePath, null,
                        childDir.getAbsolutePath(),  true,  false,  true);

        assertThat(result, hasSize(1));
        assertThat(result, hasItem(childFilepath));
    }

}
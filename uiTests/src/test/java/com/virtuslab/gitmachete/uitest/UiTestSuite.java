package com.virtuslab.gitmachete.uitest;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.intellij.remoterobot.RemoteRobot;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.virtuslab.gitmachete.testcommon.BaseGitRepositoryBackedIntegrationTestSuite;

public class UiTestSuite extends BaseGitRepositoryBackedIntegrationTestSuite {

  private final static String rhinoCodebase = List.of("common", "ide", "project").map(UiTestSuite::loadScript).mkString();

  private RemoteRobot remoteRobot;

  public UiTestSuite() {
    super(SETUP_WITH_SINGLE_REMOTE);
  }

  @SneakyThrows
  private static String loadScript(String baseName) {
    return new String(
        Files.readAllBytes(
            Paths.get(
                UiTestSuite.class.getResource("/" + baseName + ".rhino.js").toURI())));
  }

  @Before
  public void connectToIde() {
    remoteRobot = new RemoteRobot("http://127.0.0.1:8080");
  }

  @Test
  @SneakyThrows
  public void openTabAndCountRows() {
    runJs("ide.configure()");
    runJs("ide.closeOpenedProjects()");
    runJs("ide.openProject('" + repositoryMainDir + "')");
    runJs("ide.awaitNoBackgroundTask()");
    // Note that due to how Remote Robot operates, each `runJs`/`callJs` invocation is executed in a fresh JavaScript environment.
    // Thus, we can't store any state (like the reference to the project) between invocations.
    runJs("ide.soleOpenedProject().openTab()");

    int branchRowsCount = callJs("ide.soleOpenedProject().refreshModelAndGetRowCount()");
    // There should be exactly 6 rows in the graph table, since there are 6 branches in machete file,
    // as set up via `super(SETUP_WITH_SINGLE_REMOTE)`.
    Assert.assertEquals(6, branchRowsCount);

    runJs("ide.soleOpenedProject().toggleListingCommits()");
    int branchAndCommitRowsCount = callJs("ide.soleOpenedProject().refreshModelAndGetRowCount()");
    // 6 branch rows + 7 commit rows
    Assert.assertEquals(13, branchAndCommitRowsCount);
  }

  @After
  public void closeIde() {
    runJs("ide.close()");
  }

  private void runJs(@Language("JS") String statement) {
    remoteRobot.runJs(rhinoCodebase + statement, /* runInEdt */ false);
  }

  private <T extends Serializable> T callJs(@Language("JS") String expression) {
    return remoteRobot.callJs(rhinoCodebase + expression, /* runInEdt */ false);
  }
}

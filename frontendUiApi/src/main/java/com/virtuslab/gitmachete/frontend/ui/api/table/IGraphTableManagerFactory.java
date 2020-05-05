package com.virtuslab.gitmachete.frontend.ui.api.table;

import com.intellij.openapi.project.Project;

import com.virtuslab.gitmachete.frontend.ui.api.root.IGitRepositorySelectionProvider;

public interface IGraphTableManagerFactory {
  IGraphTableManager create(BaseGraphTable graphTable, Project project,
      IGitRepositorySelectionProvider gitRepositorySelectionProvider);
}

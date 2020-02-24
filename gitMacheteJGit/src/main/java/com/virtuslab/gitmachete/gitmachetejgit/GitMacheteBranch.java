package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitcore.api.GitCoreException;
import com.virtuslab.gitcore.api.IAncestorityChecker;
import com.virtuslab.gitcore.api.IGitCoreBranchTrackingStatus;
import com.virtuslab.gitcore.api.IGitCoreCommit;
import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteCommit;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMergeParameters;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToOriginStatus;
import com.virtuslab.gitmachete.gitmacheteapi.SyncToParentStatus;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.Data;

@Data
public class GitMacheteBranch implements IGitMacheteBranch {
  private final IGitCoreLocalBranch coreLocalBranch;
  private final IGitMacheteBranch upstreamBranch;
  private final String customAnnotation;
  private final IAncestorityChecker ancestorityChecker;
  private final List<IGitMacheteBranch> childBranches = new LinkedList<>();

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }

  @Override
  public String getName() {
    return coreLocalBranch.getName();
  }

  @Override
  public Optional<IGitMacheteBranch> getUpstreamBranch() {
    return Optional.ofNullable(upstreamBranch);
  }

  public List<IGitMacheteCommit> computeCommits() throws GitMacheteException {
    if (upstreamBranch == null) {
      return List.of();
    }

    try {
      Optional<IGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
      if (forkPoint.isEmpty()) {
        return List.of();
      }

      return translateIGitCoreCommitListToIGitMacheteCommitList(
          coreLocalBranch.computeCommitsUntil(forkPoint.get()));
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public IGitMacheteCommit getPointedCommit() throws GitMacheteException {
    try {
      return new GitMacheteCommit(coreLocalBranch.getPointedCommit());
    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  public List<IGitMacheteBranch> getDownstreamBranches() {
    return childBranches;
  }

  public SyncToOriginStatus computeSyncToOriginStatus() throws GitMacheteException {
    try {
      Optional<IGitCoreBranchTrackingStatus> ts = coreLocalBranch.computeRemoteTrackingStatus();
      if (ts.isEmpty()) {
        return SyncToOriginStatus.Untracked;
      }

      if (ts.get().getAhead() > 0 && ts.get().getBehind() > 0) return SyncToOriginStatus.Diverged;
      else if (ts.get().getAhead() > 0) return SyncToOriginStatus.Ahead;
      else if (ts.get().getBehind() > 0) return SyncToOriginStatus.Behind;
      else return SyncToOriginStatus.InSync;

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  @Override
  public IGitRebaseParameters computeRebaseParameters() throws GitMacheteException {
    try {
      if (getUpstreamBranch().isEmpty()) {
        throw new GitMacheteException(
            MessageFormat.format(
                "Can not get rebase parameters for root branch \"{0}\"", getName()));
      }

      var forkPoint = coreLocalBranch.computeForkPoint();
      if (forkPoint.isEmpty()) {
        throw new GitMacheteException(
            MessageFormat.format("Can not find fork point for branch \"{0}\"", getName()));
      }

      return new GitRebaseParameters(
          /*currentBranch*/ this,
          getUpstreamBranch().get().getPointedCommit(),
          new GitMacheteCommit(forkPoint.get()));

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  public SyncToParentStatus computeSyncToParentStatus() throws GitMacheteException {
    try {
      if (upstreamBranch == null) {
        return SyncToParentStatus.InSync;
      }

      var upstreamBranchPointedCommitHash = upstreamBranch.getPointedCommit().getHash();
      var myPointedCommitHash = coreLocalBranch.getPointedCommit().getHash().getHashString();

      if (myPointedCommitHash.equals(upstreamBranchPointedCommitHash)) {
        if (coreLocalBranch.hasJustBeenCreated()) {
          return SyncToParentStatus.InSync;
        } else {
          return SyncToParentStatus.Merged;
        }
      } else {
        Optional<IGitCoreCommit> forkPoint = coreLocalBranch.computeForkPoint();
        boolean isParentAncestorOfChild =
            ancestorityChecker.check(upstreamBranchPointedCommitHash, myPointedCommitHash);

        if (isParentAncestorOfChild) {
          if (forkPoint.isEmpty()
              || !forkPoint
                  .get()
                  .getHash()
                  .getHashString()
                  .equals(upstreamBranchPointedCommitHash)) {
            return SyncToParentStatus.InSyncButForkPointOff;
          } else {
            return SyncToParentStatus.InSync;
          }
        } else {
          boolean isChildAncestorOfParent =
              ancestorityChecker.check(myPointedCommitHash, upstreamBranchPointedCommitHash);

          if (isChildAncestorOfParent) {
            return SyncToParentStatus.Merged;
          } else {
            return SyncToParentStatus.OutOfSync;
          }
        }
      }

    } catch (GitCoreException e) {
      throw new GitMacheteException(e);
    }
  }

  private List<IGitMacheteCommit> translateIGitCoreCommitListToIGitMacheteCommitList(
      List<IGitCoreCommit> list) throws GitCoreException {
    var l = new LinkedList<IGitMacheteCommit>();

    for (var c : list) {
      l.add(new GitMacheteCommit(c));
    }

    return l;
  }

  @Override
  public IGitMergeParameters getMergeParameters() throws GitMacheteException {
    if (getUpstreamBranch().isEmpty()) {
      throw new GitMacheteException(
          MessageFormat.format("Can not get merge parameters for root branch \"{0}\"", getName()));
    }

    return new GitMergeParameters(/*currentBranch*/ this, getUpstreamBranch().get());
  }
}

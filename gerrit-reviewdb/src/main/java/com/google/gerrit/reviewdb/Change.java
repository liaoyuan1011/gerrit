// Copyright (C) 2008 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.reviewdb;

import com.google.gwtorm.client.Column;

import java.sql.Timestamp;

/**
 * A change proposed to be merged into a {@link Branch}.
 * <p>
 * The data graph rooted below a Change can be quite complex:
 *
 * <pre>
 *   {@link Change}
 *     |
 *     +- {@link ChangeMessage}: &quot;cover letter&quot; or general comment.
 *     |
 *     +- {@link PatchSet}: a single variant of this change.
 *          |
 *          +- {@link PatchSetApproval}: a +/- vote on the change's current state.
 *          |
 *          +- {@link PatchSetAncestor}: parents of this change's commit.
 *          |
 *          +- {@link PatchLineComment}: comment about a specific line
 * </pre>
 * <p>
 * <h5>PatchSets</h5>
 * <p>
 * Every change has at least one PatchSet. A change starts out with one
 * PatchSet, the initial proposal put forth by the change owner. This
 * {@link Account} is usually also listed as the author and committer in the
 * PatchSetInfo.
 * <p>
 * The {@link PatchSetAncestor} entities are a mirror of the Git commit
 * metadata, providing access to the information without needing direct
 * accessing Git. These entities are actually legacy artifacts from Gerrit 1.x
 * and could be removed, replaced by direct RevCommit access.
 * <p>
 * Each PatchSet contains zero or more Patch records, detailing the file paths
 * impacted by the change (otherwise known as, the file paths the author
 * added/deleted/modified). Sometimes a merge commit can contain zero patches,
 * if the merge has no conflicts, or has no impact other than to cut off a line
 * of development.
 * <p>
 * Each PatchLineComment is a draft or a published comment about a single line
 * of the associated file. These are the inline comment entities created by
 * users as they perform a review.
 * <p>
 * When additional PatchSets appear under a change, these PatchSets reference
 * <i>replacement</i> commits; alternative commits that could be made to the
 * project instead of the original commit referenced by the first PatchSet.
 * <p>
 * A change has at most one current PatchSet. The current PatchSet is updated
 * when a new replacement PatchSet is uploaded. When a change is submitted, the
 * current patch set is what is merged into the destination branch.
 * <p>
 * <h5>ChangeMessage</h5>
 * <p>
 * The ChangeMessage entity is a general free-form comment about the whole
 * change, rather than PatchLineComment's file and line specific context. The
 * ChangeMessage appears at the start of any email generated by Gerrit, and is
 * shown on the change overview page, rather than in a file-specific context.
 * Users often use this entity to describe general remarks about the overall
 * concept proposed by the change.
 * <p>
 * <h5>PatchSetApproval</h5>
 * <p>
 * PatchSetApproval entities exist to fill in the <i>cells</i> of the approvals
 * table in the web UI. That is, a single PatchSetApproval record's key is the
 * tuple {@code (PatchSet,Account,ApprovalCategory)}. Each PatchSetApproval
 * carries with it a small score value, typically within the range -2..+2.
 * <p>
 * If an Account has created only PatchSetApprovals with a score value of 0, the
 * Change shows in their dashboard, and they are said to be CC'd (carbon copied)
 * on the Change, but are not a direct reviewer. This often happens when an
 * account was specified at upload time with the {@code --cc} command line flag,
 * or have published comments, but left the approval scores at 0 ("No Score").
 * <p>
 * If an Account has one or more PatchSetApprovals with a score != 0, the Change
 * shows in their dashboard, and they are said to be an active reviewer. Such
 * individuals are highlighted when notice of a replacement patch set is sent,
 * or when notice of the change submission occurs.
 */
public final class Change extends AbstractEntity {
  public static class Id extends AbstractEntity.Id {
    private static final long serialVersionUID = 1L;

    public Id() {
      super();
    }

    public Id(final int id) {
      super(id);
    }

    /** Parse a Change.Id out of a string representation. */
    public static Id parse(final String str) {
      final Id r = new Id();
      r.fromString(str);
      return r;
    }

    public static Id fromRef(final String ref) {
      return PatchSet.Id.fromRef(ref).getParentKey();
    }
  }
  /** Locally assigned unique identifier of the change */
  @Column(id = 1)
  protected Id changeId;

  /** Globally assigned unique identifier of the change */
  @Column(id = 2)
  protected Key changeKey;

  /** The total number of {@link PatchSet} children in this Change. */
  @Column(id = 11)
  protected int nbrPatchSets;

  /** The current patch set. */
  @Column(id = 12)
  protected int currentPatchSetId;

  /** Subject from the current patch set. */
  @Column(id = 13)
  protected String subject;

  /** Topic name assigned by the user, if any. */
  @Column(id = 14, notNull = false)
  protected String topic;

  /**
   * Null if the change has never been tested.
   * Empty if it has been tested but against a branch that does
   * not exist.
   */
  @Column(id = 15, notNull = false)
  protected RevId lastSha1MergeTested;

  @Column(id = 16)
  protected boolean mergeable;

  protected Change() {
  }

  public Change(final Change.Key newKey, final Change.Id newId,
      final Account.Id ownedBy, final Branch.NameKey forBranch) {
    changeKey = newKey;
    changeId = newId;
    createdOn = new Timestamp(System.currentTimeMillis());
    lastUpdatedOn = createdOn;
    owner = ownedBy;
    dest = forBranch;
    setStatus(Status.NEW);
    setLastSha1MergeTested(null);
  }

  /** Legacy 32 bit integer identity for a change. */
  public Change.Id getId() {
    return changeId;
  }

  /** Legacy 32 bit integer identity for a change. */
  public int getChangeId() {
    return changeId.get();
  }

  /** The Change-Id tag out of the initial commit, or a natural key. */
  public Change.Key getKey() {
    return changeKey;
  }

  public void setKey(final Change.Key k) {
    changeKey = k;
  }

  public String getSubject() {
    return subject;
  }

  /** Get the id of the most current {@link PatchSet} in this change. */
  public PatchSet.Id currentPatchSetId() {
    if (currentPatchSetId > 0) {
      return new PatchSet.Id(changeId, currentPatchSetId);
    }
    return null;
  }

  public void setCurrentPatchSet(final PatchSetInfo ps) {
    currentPatchSetId = ps.getKey().get();
    subject = ps.getSubject();
  }

  /**
   * Allocate a new PatchSet id within this change.
   * <p>
   * <b>Note: This makes the change dirty. Call update() after.</b>
   */
  public void nextPatchSetId() {
    ++nbrPatchSets;
  }

  public PatchSet.Id currPatchSetId() {
    return new PatchSet.Id(changeId, nbrPatchSets);
  }

  public Status getStatus() {
    return Status.forCode(status);
  }

  public void setStatus(final Status newStatus) {
    open = newStatus.isOpen();
    status = newStatus.getCode();
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public RevId getLastSha1MergeTested() {
    return lastSha1MergeTested;
  }

  public void setLastSha1MergeTested(RevId lastSha1MergeTested) {
    this.lastSha1MergeTested = lastSha1MergeTested;
  }

  public boolean isMergeable() {
    return mergeable;
  }

  public void setMergeable(boolean mergeable) {
    this.mergeable = mergeable;
  }
}
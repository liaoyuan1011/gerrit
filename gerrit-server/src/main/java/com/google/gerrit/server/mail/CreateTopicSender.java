// Copyright (C) 2009 The Android Open Source Project
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

package com.google.gerrit.server.mail;

import com.google.gerrit.reviewdb.Change;
import com.google.gerrit.reviewdb.AccountProjectWatch.NotifyType;
import com.google.gerrit.reviewdb.Topic;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.ssh.SshInfo;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


public class CreateTopicSender extends NewTopicSender {
  public static interface Factory {
    public CreateTopicSender create(Change change);
  }

  private final GroupCache groupCache;

  @Inject
  public CreateTopicSender(EmailArguments ea, SshInfo sshInfo,
      GroupCache groupCache, @Assisted Topic t) {
    super(ea, sshInfo, t);
    this.groupCache = groupCache;
  }

  @Override
  protected void init() throws EmailException {
    super.init();

    bccWatchers();
  }

  private void bccWatchers() {
    bccWatchers(groupCache, NotifyType.NEW_CHANGES);
  }
}

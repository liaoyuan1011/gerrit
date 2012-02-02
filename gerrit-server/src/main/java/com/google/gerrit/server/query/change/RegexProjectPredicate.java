// Copyright (C) 2010 The Android Open Source Project
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

package com.google.gerrit.server.query.change;

import com.google.gerrit.reviewdb.Change;
import com.google.gerrit.reviewdb.Project;
import com.google.gerrit.reviewdb.ReviewDb;
import com.google.gerrit.server.query.AbstractRegexProjectPredicate;
import com.google.gwtorm.client.OrmException;
import com.google.inject.Provider;

class RegexProjectPredicate extends AbstractRegexProjectPredicate<ChangeData> {

  RegexProjectPredicate(Provider<ReviewDb> dbProvider, String re) {
    super(dbProvider, re);
  }

  @Override
  public boolean match(final ChangeData object) throws OrmException {
    Change change = object.change(dbProvider);
    if (change == null) {
      return false;
    }

    Project.NameKey p = change.getDest().getParentKey();
    return pattern.run(p.get());
  }

}

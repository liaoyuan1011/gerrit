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

package com.google.gerrit.client.topics;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.client.rpc.ScreenLoadCallback;
import com.google.gerrit.client.topics.TopicTable.ApprovalViewType;
import com.google.gerrit.client.ui.Hyperlink;
import com.google.gerrit.client.ui.Screen;
import com.google.gerrit.common.data.SingleListTopicInfo;
import com.google.gerrit.common.data.TopicInfo;
import com.google.gerrit.reviewdb.AccountGeneralPreferences;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwtexpui.globalkey.client.KeyCommand;

import java.util.List;


public abstract class PagedSingleListScreen extends Screen {
  protected static final String MIN_SORTKEY = "";
  protected static final String MAX_SORTKEY = "z";

  protected final int pageSize;
  private TopicTable table;
  private TopicTable.Section section;
  protected Hyperlink prev;
  protected Hyperlink next;
  protected List<TopicInfo> changes;

  protected final String anchorPrefix;
  protected boolean useLoadPrev;
  protected String pos;

  protected PagedSingleListScreen(final String anchorToken,
      final String positionToken) {
    anchorPrefix = anchorToken;
    useLoadPrev = positionToken.startsWith("p,");
    pos = positionToken.substring(2);

    if (Gerrit.isSignedIn()) {
      final AccountGeneralPreferences p =
          Gerrit.getUserAccount().getGeneralPreferences();
      final short m = p.getMaximumPageSize();
      pageSize = 0 < m ? m : AccountGeneralPreferences.DEFAULT_PAGESIZE;
    } else {
      pageSize = AccountGeneralPreferences.DEFAULT_PAGESIZE;
    }
  }

  @Override
  protected void onInitUI() {
    super.onInitUI();
    prev = new Hyperlink(com.google.gerrit.client.changes.Util.C.pagedChangeListPrev(), true, "");
    prev.setVisible(false);

    next = new Hyperlink(com.google.gerrit.client.changes.Util.C.pagedChangeListNext(), true, "");
    next.setVisible(false);

    table = new TopicTable(true) {
      {
        keysNavigation.add(new DoLinkCommand(0, 'p', com.google.gerrit.client.changes.Util.C
            .changeTablePagePrev(), prev));
        keysNavigation.add(new DoLinkCommand(0, 'n', com.google.gerrit.client.changes.Util.C
            .changeTablePageNext(), next));
      }
    };
    section = new TopicTable.Section(null, ApprovalViewType.STRONGEST, null);

    table.addSection(section);
    table.setSavePointerId(anchorPrefix);
    add(table);

    final HorizontalPanel buttons = new HorizontalPanel();
    buttons.setStyleName(Gerrit.RESOURCES.css().changeTablePrevNextLinks());
    buttons.add(prev);
    buttons.add(next);
    add(buttons);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    if (useLoadPrev) {
      loadPrev();
    } else {
      loadNext();
    }
  }

  @Override
  public void registerKeys() {
    super.registerKeys();
    table.setRegisterKeys(true);
  }

  protected abstract void loadPrev();

  protected abstract void loadNext();

  protected AsyncCallback<SingleListTopicInfo> loadCallback() {
    return new ScreenLoadCallback<SingleListTopicInfo>(this) {
      @Override
      protected void preDisplay(final SingleListTopicInfo result) {
        display(result);
      }
    };
  }

  protected void display(final SingleListTopicInfo result) {
    changes = result.getTopics();

    if (!changes.isEmpty()) {
      final TopicInfo f = changes.get(0);
      final TopicInfo l = changes.get(changes.size() - 1);

      prev.setTargetHistoryToken(anchorPrefix + ",p," + f.getSortKey());
      next.setTargetHistoryToken(anchorPrefix + ",n," + l.getSortKey());

      if (useLoadPrev) {
        prev.setVisible(!result.isAtEnd());
        next.setVisible(!MIN_SORTKEY.equals(pos));
      } else {
        prev.setVisible(!MAX_SORTKEY.equals(pos));
        next.setVisible(!result.isAtEnd());
      }
    }

    table.setAccountInfoCache(result.getAccounts());
    section.display(result.getTopics());
    table.finishDisplay();
  }

  private static final class DoLinkCommand extends KeyCommand {
    private final Hyperlink link;

    private DoLinkCommand(int mask, char key, String help, Hyperlink l) {
      super(mask, key, help);
      link = l;
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      if (link.isVisible()) {
        History.newItem(link.getTargetHistoryToken());
      }
    }
  }
}
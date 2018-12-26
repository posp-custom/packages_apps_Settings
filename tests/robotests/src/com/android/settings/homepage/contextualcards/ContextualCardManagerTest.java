/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;

import com.android.settings.homepage.contextualcards.conditional.ConditionFooterContextualCard;
import com.android.settings.homepage.contextualcards.conditional.ConditionHeaderContextualCard;
import com.android.settings.homepage.contextualcards.conditional.ConditionalContextualCard;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardManagerTest {

    private static final String TEST_SLICE_URI = "context://test/test";
    private static final String TEST_SLICE_NAME = "test_name";

    @Mock
    ContextualCardUpdateListener mListener;

    private Context mContext;
    private ContextualCardManager mManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final ContextualCardsFragment fragment = new ContextualCardsFragment();
        mManager = new ContextualCardManager(mContext, fragment.getSettingsLifecycle(),
                null /* bundle */);
    }

    @Test
    public void sortCards_hasConditionalAndSliceCards_conditionalShouldAlwaysBeTheLast() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ConditionalContextualCard.Builder().build());
        cards.add(buildContextualCard(TEST_SLICE_URI));

        final List<ContextualCard> sortedCards = mManager.sortCards(cards);

        assertThat(sortedCards.get(cards.size() - 1).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }

    @Test
    public void onContextualCardUpdated_emptyMapWithExistingCards_shouldOnlyKeepConditionalCard() {
        mManager.mContextualCards.add(new ConditionalContextualCard.Builder().build());
        mManager.mContextualCards.add(
                buildContextualCard(TEST_SLICE_URI));
        mManager.setListener(mListener);

        //Simulate database returns no contents.
        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }

    @Test
    public void onContextualCardUpdated_hasEmptyMap_shouldKeepConditionalHeaderCard() {
        mManager.mContextualCards.add(new ConditionHeaderContextualCard.Builder().build());
        mManager.setListener(mListener);

        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL_HEADER);
    }

    @Test
    public void onContextualCardUpdated_hasEmptyMap_shouldKeepConditionalFooterCard() {
        mManager.mContextualCards.add(new ConditionFooterContextualCard.Builder().build());
        mManager.setListener(mListener);

        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL_FOOTER);
    }

    @Test
    public void onFinishCardLoading_fastLoad_shouldCallOnContextualCardUpdated() {
        mManager.mStartTime = System.currentTimeMillis();
        final ContextualCardManager manager = spy(mManager);
        doNothing().when(manager).onContextualCardUpdated(anyMap());

        manager.onFinishCardLoading(new ArrayList<>());
        verify(manager).onContextualCardUpdated(nullable(Map.class));
    }

    @Test
    public void onFinishCardLoading_slowLoad_shouldSkipOnContextualCardUpdated() {
        mManager.mStartTime = 0;
        final ContextualCardManager manager = spy(mManager);
        doNothing().when(manager).onContextualCardUpdated(anyMap());

        manager.onFinishCardLoading(new ArrayList<>());
        verify(manager, never()).onContextualCardUpdated(anyMap());
    }

    @Test
    public void onFinishCardLoading_newLaunch_twoLoadedCards_shouldShowTwoCards() {
        mManager.mStartTime = System.currentTimeMillis();
        mManager.setListener(mListener);
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(buildContextualCard(TEST_SLICE_URI));
        cards.add(buildContextualCard(TEST_SLICE_URI));

        mManager.onFinishCardLoading(cards);

        assertThat(mManager.mContextualCards).hasSize(2);
    }

    @Test
    public void onFinishCardLoading_hasSavedCard_shouldOnlyShowSavedCard() {
        mManager.setListener(mListener);
        final List<String> savedCardNames = new ArrayList<>();
        savedCardNames.add(TEST_SLICE_NAME);
        mManager.mIsFirstLaunch = false;
        mManager.mSavedCards = savedCardNames;
        final ContextualCard newCard =
                new ContextualCard.Builder()
                        .setName("test_name2")
                        .setCardType(ContextualCard.CardType.SLICE)
                        .setSliceUri(Uri.parse("content://test/test2"))
                        .build();
        final List<ContextualCard> loadedCards = new ArrayList<>();
        loadedCards.add(buildContextualCard(TEST_SLICE_URI));
        loadedCards.add(newCard);

        mManager.onFinishCardLoading(loadedCards);

        final List<String> actualCards = mManager.mContextualCards.stream()
                .map(ContextualCard::getName)
                .collect(Collectors.toList());
        final List<String> expectedCards = Arrays.asList(TEST_SLICE_NAME);
        assertThat(actualCards).containsExactlyElementsIn(expectedCards);
    }

    @Test
    public void onFinishCardLoading_reloadData_shouldOnlyShowOldCard() {
        mManager.setListener(mListener);
        mManager.mIsFirstLaunch = false;
        //old card
        mManager.mContextualCards.add(buildContextualCard(TEST_SLICE_URI));
        final ContextualCard newCard =
                new ContextualCard.Builder()
                        .setName("test_name2")
                        .setCardType(ContextualCard.CardType.SLICE)
                        .setSliceUri(Uri.parse("content://test/test2"))
                        .build();
        final List<ContextualCard> loadedCards = new ArrayList<>();
        loadedCards.add(buildContextualCard(TEST_SLICE_URI));
        loadedCards.add(newCard);

        mManager.onFinishCardLoading(loadedCards);

        final List<String> actualCards = mManager.mContextualCards.stream()
                .map(ContextualCard::getName)
                .collect(Collectors.toList());
        final List<String> expectedCards = Arrays.asList(TEST_SLICE_NAME);
        assertThat(actualCards).containsExactlyElementsIn(expectedCards);
    }

    private ContextualCard buildContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName(TEST_SLICE_NAME)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }
}
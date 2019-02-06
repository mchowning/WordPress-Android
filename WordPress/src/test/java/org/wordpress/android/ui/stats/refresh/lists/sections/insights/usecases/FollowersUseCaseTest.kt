package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FetchMode
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.store.stats.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

class FollowersUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var statsUtilsWrapper: StatsUtilsWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: FollowersUseCase
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val dateSubscribed = Date(10)
    private val sinceLabel = "4 days"
    private val totalCount = 50
    private val wordPressLabel = "wordpress"
    private val blockPageSize = 6
    private val viewAllPageSize = 10
    private val blockInitialMode = FetchMode.Paged(blockPageSize, false)
    private val viewAllInitialLoadMode = FetchMode.Paged(viewAllPageSize, true)
    private val viewAllMoreLoadMode = FetchMode.Paged(viewAllPageSize, true)
    val message = "Total followers count is 50"
    @Before
    fun setUp() {
        useCase = FollowersUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsUtilsWrapper,
                resourceProvider,
                tracker,
                BLOCK
        )
        whenever(statsUtilsWrapper.getSinceLabelLowerCase(dateSubscribed)).thenReturn(sinceLabel)
        whenever(resourceProvider.getString(any())).thenReturn(wordPressLabel)
        whenever(resourceProvider.getString(eq(R.string.stats_followers_count_message), any(), any())).thenReturn(
                message
        )
    }

    @Test
    fun `maps followers from selected tab to UI model and select empty tab`() = test {
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        FollowersModel(
                                totalCount,
                                listOf(FollowerModel(avatar, user, url, dateSubscribed)),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        val tabsItem = (result as BlockList).assertSelectedFollowers(position = 0)

        tabsItem.onTabSelected(1)

        val updatedResult = loadFollowers(refresh)

        (updatedResult as BlockList).assertEmptyTabSelected(1)
    }

    @Test
    fun `maps email followers to UI model`() = test {
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        FollowersModel(
                                totalCount,
                                listOf(FollowerModel(avatar, user, url, dateSubscribed)),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        val tabsItem = (result as BlockList).assertEmptyTabSelected(0)

        tabsItem.onTabSelected(1)
        val updatedResult = loadFollowers(refresh)
        (updatedResult as BlockList).assertSelectedFollowers(position = 1)
    }

    @Test
    fun `maps empty followers to UI model`() = test {
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).assertEmpty()
    }

    @Test
    fun `maps WPCOM error item to UI model`() = test {
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchWpComFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh)

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    @Test
    fun `maps email error item to UI model`() = test {
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchWpComFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, blockInitialMode)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadFollowers(refresh)

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    @Test
    fun `maps email followers to UI model in the view all mode`() = test {
        useCase = FollowersUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsUtilsWrapper,
                resourceProvider,
                tracker,
                VIEW_ALL
        )

        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, viewAllInitialLoadMode)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, viewAllInitialLoadMode)).thenReturn(
                OnStatsFetched(
                        FollowersModel(
                                totalCount,
                                List(10) { FollowerModel(avatar, user, url, dateSubscribed) },
                                hasMore = true
                        )
                )
        )

        whenever(insightsStore.fetchWpComFollowers(site, viewAllMoreLoadMode, true)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, viewAllMoreLoadMode, true)).thenReturn(
                OnStatsFetched(
                        FollowersModel(
                                totalCount,
                                List(11) { FollowerModel(avatar, user, url, dateSubscribed) },
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        val tabsItem = (result as BlockList).assertEmptyTabSelectedViewAllMode(0)

        tabsItem.onTabSelected(1)
        var updatedResult = loadFollowers(refresh)
        val button = (updatedResult as BlockList).assertViewAllFollowersFirstLoad(position = 1)

        button.navigateAction.click()
        delay(1000)
        updatedResult = useCase.liveData.value!! as BlockList
        updatedResult.assertViewAllFollowersSecondLoad()
    }

    private suspend fun loadFollowers(refresh: Boolean, forced: Boolean = false): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_followers)
    }

    private fun BlockList.assertViewAllFollowersFirstLoad(position: Int): Link {
        assertThat(this.items).hasSize(15)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem
        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_followers_email)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(this.items[2]).isEqualTo(Information("Total followers count is 50"))
        assertThat(this.items[3]).isEqualTo(
                Header(
                        string.stats_follower_label,
                        string.stats_follower_since_label
                )
        )
        val follower = this.items[4] as ListItemWithIcon
        assertThat(follower.iconUrl).isEqualTo(avatar)
        assertThat(follower.iconStyle).isEqualTo(AVATAR)
        assertThat(follower.text).isEqualTo(user)
        assertThat(follower.value).isEqualTo(sinceLabel)
        assertThat(follower.showDivider).isEqualTo(true)

        assertThat(this.items[13] is ListItemWithIcon).isTrue()

        val button = this.items[14] as Link
        assertThat(button.text).isEqualTo(R.string.stats_insights_load_more)

        return button
    }

    private fun BlockList.assertViewAllFollowersSecondLoad() {
        assertThat(this.items).hasSize(15)

        val follower = this.items[14] as ListItemWithIcon
        assertThat(follower.showDivider).isEqualTo(false)
    }

    private fun BlockList.assertSelectedFollowers(position: Int): TabsItem {
        assertThat(this.items).hasSize(5)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem
        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_followers_email)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(this.items[2]).isEqualTo(Information("Total followers count is 50"))
        assertThat(this.items[3]).isEqualTo(
                Header(
                        string.stats_follower_label,
                        string.stats_follower_since_label
                )
        )
        val follower = this.items[4] as ListItemWithIcon
        assertThat(follower.iconUrl).isEqualTo(avatar)
        assertThat(follower.iconStyle).isEqualTo(AVATAR)
        assertThat(follower.text).isEqualTo(user)
        assertThat(follower.value).isEqualTo(sinceLabel)
        assertThat(follower.showDivider).isEqualTo(false)
        return tabsItem
    }

    private fun BlockList.assertEmptyTabSelectedViewAllMode(position: Int): TabsItem {
        assertThat(this.items).hasSize(4)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_followers_email)
        assertThat(this.items[2]).isEqualTo(Empty())
        return tabsItem
    }

    private fun BlockList.assertEmptyTabSelected(position: Int): TabsItem {
        assertThat(this.items).hasSize(3)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_followers_email)
        assertThat(this.items[2]).isEqualTo(Empty())
        return tabsItem
    }

    private fun BlockList.assertEmpty() {
        assertThat(this.items).hasSize(2)
        assertTitle(this.items[0])
        assertThat(this.items[1]).isEqualTo(Empty())
    }
}

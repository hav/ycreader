package se.ntlv.newsbringer.comments

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.applyDimension
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import org.jetbrains.anko.*
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.application.GlobalDependency
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.DataCommentsThread
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.thisShouldNeverHappen
import kotlin.LazyThreadSafetyMode.NONE

class CommentsActivity : AppCompatActivity() {

    val database: Database by lazy(NONE) { GlobalDependency.database }

    private lateinit var mAdapter: CommentsAdapterWithHeader
    private lateinit var mPresenter: CommentsPresenter
    private lateinit var mUiBinder: UiBinder

    private val dataTag = "CommentsActivity.data"

    private val mItemId by lazy(NONE) { intent.data.getQueryParameterOrThrow("id") }

    //ANDROID ACTIVITY CALLBACKS
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_linear_vertical_content)

        setSupportActionBar(find<Toolbar>(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val data = savedInstanceState?.getParcelable<DataCommentsThread>(dataTag)

        val navigator = Navigator(this)
        val padding = applyDimension(COMPLEX_UNIT_DIP, 4f, displayMetrics).toInt()
        mAdapter = CommentsAdapterWithHeader(data, padding, { mPresenter.onHeaderClick() }, navigator)

        mUiBinder = UiBinder(this, LinearLayoutManager(this), mAdapter)
        val interactor = CommentsInteractor(database, mItemId, navigator, data?.base)
        mPresenter = CommentsPresenter(mUiBinder, interactor)
    }

    override fun onStart() {
        super.onStart()
        mUiBinder.start()
    }

    override fun onStop() {
        super.onStop()
        mUiBinder.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(dataTag, mAdapter.data)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.destroy()
        mUiBinder.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.comments, menu)
        val refreshImage = ImageView(this)
        refreshImage.padding = dip(8)
        refreshImage.imageResource = R.drawable.ic_refresh_24dp
        val refreshItem = menu.findItem(R.id.refresh)
        refreshItem.actionView = refreshImage
        refreshItem.actionView.onClick { menu.performIdentifierAction(refreshItem.itemId, 0) }
        mUiBinder.refreshButtonManager = RefreshButtonAnimator(refreshItem, refreshImage)
        mUiBinder.refreshButtonManager = RefreshButtonAnimator(menu.findItem(R.id.refresh), refreshImage)
        mPresenter.onViewReady()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.refresh -> mPresenter.refreshData()
            R.id.share_story -> mPresenter.onShareStoryClicked()
            R.id.share_comments -> mPresenter.onShareCommentsClicked()
            R.id.open_link -> mPresenter.onHeaderClick()
            R.id.add_to_starred -> mPresenter.addToStarred()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}

private fun Uri.getQueryParameterOrThrow(paramName: String): Long {
    val asLong = getQueryParameter(paramName)?.toLong()
    return when {
        asLong == null || asLong < 0 -> thisShouldNeverHappen()
        else -> asLong
    }
}

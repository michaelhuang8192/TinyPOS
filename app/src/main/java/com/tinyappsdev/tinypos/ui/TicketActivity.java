package com.tinyappsdev.tinypos.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.tinyappsdev.tinypos.AppGlobal;
import com.tinyappsdev.tinypos.R;
import com.tinyappsdev.tinypos.data.Customer;
import com.tinyappsdev.tinypos.data.ModelHelper;
import com.tinyappsdev.tinypos.data.Ticket;
import com.tinyappsdev.tinypos.rest.ApiCallClient;
import com.tinyappsdev.tinypos.ui.BaseUI.BaseActivity;
import com.tinyappsdev.tinypos.ui.BaseUI.TicketActivityInterface;
import com.tinyappsdev.tinypos.ui.TicketFragment.TicketFoodFragment;
import com.tinyappsdev.tinypos.ui.TicketFragment.TicketInfoFragment;
import com.tinyappsdev.tinypos.ui.TicketFragment.TicketPaymentFragment;
import com.tinyappsdev.tinypos.ui.TicketFragment.TicketSearchFragment;

public class TicketActivity extends SyncableActivity implements
        TicketActivityInterface,
        SearchView.OnQueryTextListener {

    public static final String SearchFragmentTag = TicketSearchFragment.class.getSimpleName();

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private Ticket mTicket;
    private ApiCallClient.Result<Ticket> mResult;
    private String mLastSearchQuery;
    private boolean mIsSearchActive;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private Handler mHandler;
    private Runnable mSearchDelay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mHandler = new Handler();

        mSearchDelay = new Runnable() {
            @Override
            public void run() {
                sendMessage(R.id.ticketActivityOnSearchQueryChange);
            }
        };

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = (TabLayout)findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        if(savedInstanceState != null)
            mTicket = ModelHelper.fromJson(savedInstanceState.getString("mTicket"), Ticket.class);

        if(mTicket == null) {
            mTicket = new Ticket();
            mTicket.setDbRev(-1);
            Bundle bundle = getIntent().getExtras();
            if(bundle != null) mTicket.setId(bundle.getLong("ticketId"));
        }

        if(savedInstanceState == null) {
            if(mTicket.getId() == 0) {
                showSearchResults();
                sendMessage(R.id.ticketActivityOnSearchQueryChange);
            }
        }

        loadTicket(mTicket.getId());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
            case R.id.list: {
                showSearchResults();
                sendMessage(R.id.ticketActivityOnSearchQueryChange);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void showSearchResults() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SearchFragmentTag);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(fragment == null) {
            fragment = TicketSearchFragment.newInstance();
            fragmentTransaction.add(R.id.content_ticket, fragment, SearchFragmentTag);
        } else {
            fragmentTransaction.show(fragment);
        }
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_ticket, menu);

        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setOnQueryTextListener(this);

        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                showSearchResults();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mSearchView.setQuery(mLastSearchQuery, false);
                    }
                });

                mIsSearchActive = true;

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                if(mTicket.getId() > 0 || mTicket.getDbRev() == 0) {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag(SearchFragmentTag);
                    if (fragment != null) {
                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.hide(fragment);
                        fragmentTransaction.commit();
                    }
                }

                mLastSearchQuery = mSearchView.getQuery().toString();
                mIsSearchActive = false;
                mHandler.removeCallbacks(mSearchDelay);

                return true;
            }
        });

        return true;
    }

    public void openMap(View view) {
        if(mTicket.getCustomer() == null || mTicket.getCustomer().getAddress() == null) return;

        Customer customer = mTicket.getCustomer();
        String query = String.format(
                "%s,%s,%s",
                customer.getAddress(),
                customer.getCity(),
                customer.getState()
        );
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    @Override
    public Ticket getTicket() {
        return mTicket;
    }

    @Override
    public void selectTicket(long ticketId) {
        loadTicket(ticketId);
        mSearchItem.collapseActionView();
    }

    @Override
    public String getSearchQuery() {
        return mSearchView.getQuery().toString();
    }

    public void loadTicket(long ticketId) {
        if(mResult != null) mResult.cancel();
        if(ticketId == mTicket.getId() && mTicket.getDbRev() >= 0) return;

        mTicket = new Ticket();
        mTicket.setId(ticketId);

        if(ticketId == 0) {
            sendMessage(R.id.ticketActivityOnTicketUpdate);
            return;
        }

        mTicket.setDbRev(-1);
        mResult = AppGlobal.getInstance().getUiApiCallClient().makeCall(
                "/Ticket/getDoc?_id=" + ticketId,
                null,
                Ticket.class,
                new ApiCallClient.OnResultListener<Ticket>() {
                    @Override
                    public void onResult(ApiCallClient.Result<Ticket> result) {
                        if(result.error != null || result.data == null) {
                            Toast.makeText(getApplicationContext(),
                                    String.format(getString(R.string.open_order_error), mTicket.getId()),
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            mTicket = result.data;
                            sendMessage(R.id.ticketActivityOnTicketUpdate);
                        }
                    }
                }
        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mResult != null) mResult.cancel();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mHandler.removeCallbacks(mSearchDelay);
        if(mIsSearchActive) mHandler.postDelayed(mSearchDelay, 300);

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return onQueryTextSubmit(newText);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0)
                return TicketInfoFragment.newInstance();
            else if(position == 1)
                return TicketFoodFragment.newInstance();
            else if(position == 2)
                return TicketPaymentFragment.newInstance();

            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }


        public CharSequence getPageTitle(int position) {
            if(position == 0)
                return getString(R.string.title_ticket_info_fragment);
            else if(position == 1)
                return getString(R.string.title_ticket_food_fragment);
            else if(position == 2)
                return getString(R.string.title_ticket_payment_fragment);

            return null;
        }

    }

}

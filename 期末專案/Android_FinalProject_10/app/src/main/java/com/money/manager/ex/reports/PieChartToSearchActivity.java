package com.money.manager.ex.reports;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentTransaction;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.money.manager.ex.Constants;
import com.money.manager.ex.R;
import com.money.manager.ex.common.AllDataListFragment;
import com.money.manager.ex.common.MmxBaseFragmentActivity;
import com.money.manager.ex.core.UIHelper;
import com.money.manager.ex.database.QueryAllData;
import com.money.manager.ex.database.WhereStatementGenerator;
import com.money.manager.ex.search.CategorySub;
import com.money.manager.ex.search.SearchParameters;
import com.money.manager.ex.search.SearchParametersFragment;

public class PieChartToSearchActivity extends MmxBaseFragmentActivity {

    public static final String EXTRA_SEARCH_PARAMETERS = "ToSearchActivity:SearchCriteria";

    /**
     * Indicates whether to show the account headers in search results.
     */
    public boolean ShowAccountHeaders = true;

    private boolean mIsDualPanel = false;
    private SearchParametersFragment mSearchParametersFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        SearchParametersFragment searchParametersFragment = getSearchFragment();
        if (!searchParametersFragment.isAdded()) {
            // set dual panel
            LinearLayout fragmentDetail = (LinearLayout) findViewById(R.id.fragmentDetail);
            mIsDualPanel = fragmentDetail != null && fragmentDetail.getVisibility() == View.VISIBLE;
        }
        // reconfigure the toolbar event
//        showStandardToolbarActions(getToolbar(), R.id.action_cancel, R.id.action_search);
        setDisplayHomeAsUpEnabled(true);

        handleSearchRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AllDataListFragment fragment;
        fragment = (AllDataListFragment) getSupportFragmentManager()
                .findFragmentByTag(AllDataListFragment.class.getSimpleName());
        if (fragment != null && fragment.isVisible()) {
            fragment.loadData();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // handled in the search fragment
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        UIHelper ui = new UIHelper(this);

        // Add Search icon.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem item = menu.findItem(R.id.searchMenuItem);
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(ui.getIcon(GoogleMaterial.Icon.gmd_search));
        // show this menu item last

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.searchMenuItem:
                //performSearch();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Public

    private SearchParametersFragment createSearchFragment() {
        SearchParametersFragment searchParametersFragment = SearchParametersFragment.createInstance();

        // add to stack
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentMain, searchParametersFragment, SearchParametersFragment.class.getSimpleName())
                .commit();

        return searchParametersFragment;
    }

    private SearchParametersFragment getSearchFragment() {
        if (mSearchParametersFragment == null) {
            // try to find the search fragment
            mSearchParametersFragment = (SearchParametersFragment) getSupportFragmentManager()
                    .findFragmentByTag(SearchParametersFragment.class.getSimpleName());

            if (mSearchParametersFragment == null) {
                mSearchParametersFragment = createSearchFragment();
            }
        }
        return mSearchParametersFragment;
    }

    /**
     * Read the search request from the intent, if the activity was invoked from elsewhere.
     */
    private void handleSearchRequest() {
        Intent intent = getIntent();
        if (intent == null) return;

        CategorySub categorySub = new CategorySub();
        categorySub.categName = intent.getStringExtra("category");
        categorySub.subCategName = intent.getStringExtra("subCategory");
        Toast.makeText(this, categorySub.categName, Toast.LENGTH_LONG).show();;

        WhereStatementGenerator where = new WhereStatementGenerator();

        where.addStatement( QueryAllData.Category + "=" + categorySub.categName );
        where.addStatement( QueryAllData.Subcategory + "=" + categorySub.subCategName );


        // subcategory
        /*
        if (categorySub.subCategId != Constants.NOT_SET) {
            // Subcategory. Also check the splits.
            where.addStatement("(" +
                    "(" + QueryAllData.Subcategory + "=" + categorySub.subCategName + ") " +
                    " OR (" + categorySub.subCategName + " IN (select " + QueryAllData.Subcategory +
                    " FROM " + SplitCategory.TABLE_NAME +
                    " WHERE " + SplitCategory.TRANSID + " = " + QueryAllData.ID + ")" +
                    ")" +
                    ")");
        }*/

        String mWhere = where.getWhere();
        showSearchResultsFragment(mWhere);

    }

    public void setSearchParameters(SearchParameters parameters) {
        if (parameters == null) return;
    }


    private void showSearchResultsFragment(String where) {
        //create a fragment for search results.
        AllDataListFragment searchResultsFragment = (AllDataListFragment) this.getSupportFragmentManager()
                .findFragmentByTag(AllDataListFragment.class.getSimpleName());

        if (searchResultsFragment != null) {
            this.getSupportFragmentManager().beginTransaction()
                    .remove(searchResultsFragment)
                    .commit();
        }

        searchResultsFragment = AllDataListFragment.newInstance(Constants.NOT_SET, false);

        searchResultsFragment.showTotalsFooter();

        //create parameter bundle
        Bundle args = new Bundle();
        args.putString(AllDataListFragment.KEY_ARGUMENTS_WHERE, where);
        // Sorting
        args.putString(AllDataListFragment.KEY_ARGUMENTS_SORT,
                QueryAllData.TOACCOUNTID + ", " + QueryAllData.Date + ", " +
                        QueryAllData.TransactionType + ", " + QueryAllData.ID);
        //set arguments
        args.putAll(searchResultsFragment.getArguments());

        this.ShowAccountHeaders = true;

        //add fragment
        FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
        //animation
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_right, R.anim.slide_out_left);
        if (mIsDualPanel) {
            transaction.add(R.id.fragmentDetail, searchResultsFragment, AllDataListFragment.class.getSimpleName());
        } else {
            // transaction.remove()
            transaction.replace(R.id.fragmentMain, searchResultsFragment, AllDataListFragment.class.getSimpleName());
            transaction.addToBackStack(null);
        }
        // Commit the transaction
        transaction.commit();
    }

}
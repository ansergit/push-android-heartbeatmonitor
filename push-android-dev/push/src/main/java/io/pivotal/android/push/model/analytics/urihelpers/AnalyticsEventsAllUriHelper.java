package io.pivotal.android.push.model.analytics.urihelpers;

import android.net.Uri;

import io.pivotal.android.push.database.Database;
import io.pivotal.android.push.database.urihelpers.DeleteParams;
import io.pivotal.android.push.database.urihelpers.QueryParams;
import io.pivotal.android.push.database.urihelpers.UpdateParams;
import io.pivotal.android.push.database.urihelpers.UriHelper;
import io.pivotal.android.push.database.urihelpers.UriMatcherParams;

public class AnalyticsEventsAllUriHelper implements UriHelper {

	@Override
	public String getType() {
		return "vnd.android.cursor.dir/vnd.io.pivotal.android.push.model.analytics.AnalyticsEvent";
	}

	@Override
	public String getDefaultTableName() {
		return Database.EVENTS_TABLE_NAME;
	}
	
	@Override
	public UriMatcherParams getUriMatcherParams() {
		final UriMatcherParams uriMatcherParams = new UriMatcherParams();
		uriMatcherParams.authority = Database.AUTHORITY;
		uriMatcherParams.path = getDefaultTableName();
		return uriMatcherParams;
	}

	@Override
	public QueryParams getQueryParams(final Uri uri, final String[] projection, final String whereClause, final String[] whereArgs, final String sortOrder) {
		final QueryParams queryParams = new QueryParams();
		queryParams.uri = uri;
		queryParams.projection = projection;
		queryParams.whereClause = whereClause;
		queryParams.whereArgs = whereArgs;
		queryParams.sortOrder = sortOrder;
		return queryParams;
	}

	@Override
	public UpdateParams getUpdateParams(Uri uri, String whereClause, String[] whereArgs) {
		final UpdateParams updateParams = new UpdateParams();
		updateParams.uri = uri;
		updateParams.whereClause = whereClause;
		updateParams.whereArgs = whereArgs;
		return updateParams;
	}

	@Override
	public DeleteParams getDeleteParams(Uri uri, String whereClause, String[] whereArgs) {
		final DeleteParams deleteParams = new DeleteParams();
		deleteParams.uri = uri;
		deleteParams.whereClause = whereClause;
		deleteParams.whereArgs = whereArgs;
		return deleteParams;
	}
}

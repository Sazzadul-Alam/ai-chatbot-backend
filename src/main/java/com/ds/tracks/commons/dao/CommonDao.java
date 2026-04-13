package com.ds.tracks.commons.dao;

import com.ds.tracks.commons.models.KeyValuePair;
import com.mongodb.client.result.UpdateResult;

import java.util.List;

public interface CommonDao {
    UpdateResult update(String id, List<KeyValuePair> sets, List<KeyValuePair> pushes, String collection);
}

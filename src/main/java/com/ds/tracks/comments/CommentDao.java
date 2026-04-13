package com.ds.tracks.comments;

import java.util.List;
import java.util.Map;

public interface CommentDao {

    List<Map<String, Object>> fetch(String userId, String sourceId, String source);
}

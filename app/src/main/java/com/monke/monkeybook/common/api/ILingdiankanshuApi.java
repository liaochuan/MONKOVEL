//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.common.api;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface ILingdiankanshuApi {

    @GET
    Observable<String> getBookInfo(@Url String url);

    @GET("/cse/search")
    Observable<String> searchBook(@Query("q") String content, @Query("p") int page, @Query("s") String time);

    @GET
    Observable<String> getBookContent(@Url String url);

    @GET
    Observable<String> getChapterList(@Url String url);
}

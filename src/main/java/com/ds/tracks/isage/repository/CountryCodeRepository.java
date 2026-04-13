package com.ds.tracks.isage.repository;

import com.ds.tracks.isage.model.CountryCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CountryCodeRepository extends MongoRepository<CountryCode, String> {
    List<CountryCode> findAllByOrderByCountryAsc();
}
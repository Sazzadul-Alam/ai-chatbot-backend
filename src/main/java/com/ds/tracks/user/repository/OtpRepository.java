package com.ds.tracks.user.repository;

import com.ds.tracks.user.model.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface OtpRepository extends MongoRepository<Otp, String> {
    Optional<Otp> findByLoginIdAndReason(String email, String forgetPassword);

    Optional<Otp> findByLoginIdAndReasonAndOtp(String email, String forgetPassword, String otp);

    Optional<Otp> findByLoginIdAndReasonAndRequestToken(String email, String forgetPassword, String token);
}

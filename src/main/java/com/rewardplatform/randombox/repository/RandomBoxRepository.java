package com.rewardplatform.randombox.repository;

import com.rewardplatform.randombox.domain.RandomBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RandomBoxRepository extends JpaRepository<RandomBox, Long> {

    @Query("SELECT rb FROM RandomBox rb LEFT JOIN FETCH rb.prizes WHERE rb.isActive = true")
    List<RandomBox> findAllActiveWithPrizes();

    List<RandomBox> findByBoxTypeAndIsActiveTrue(RandomBox.BoxType boxType);
}

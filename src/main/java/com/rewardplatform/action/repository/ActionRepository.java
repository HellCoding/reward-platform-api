package com.rewardplatform.action.repository;

import com.rewardplatform.action.domain.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByIsActiveTrueOrderByOrdAsc();
    Optional<Action> findByActionType(Action.ActionType actionType);
}

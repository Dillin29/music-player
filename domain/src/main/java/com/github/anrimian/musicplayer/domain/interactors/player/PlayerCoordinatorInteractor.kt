package com.github.anrimian.musicplayer.domain.interactors.player

import com.github.anrimian.musicplayer.domain.models.composition.source.CompositionSource
import com.github.anrimian.musicplayer.domain.models.player.PlayerState
import com.github.anrimian.musicplayer.domain.models.player.events.PlayerEvent
import com.github.anrimian.musicplayer.domain.repositories.UiStateRepository
import com.github.anrimian.musicplayer.domain.utils.functions.Optional
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

class PlayerCoordinatorInteractor(
    private val playerInteractor: PlayerInteractor,
    private val uiStateRepository: UiStateRepository
) {

    private val preparedSourcesMap = HashMap<PlayerType, SourceInfo>()

    private var activePlayerType: PlayerType? = null
    private val activePlayerTypeSubject = BehaviorSubject.createDefault<Optional<PlayerType>>(
        Optional()
    )

    fun play(playerType: PlayerType, delay: Long = 0L) {
        applyPlayerType(playerType)
        playerInteractor.play(delay)
    }

    fun playAfterPrepare(playerType: PlayerType) {
        applyPlayerType(playerType)
        playerInteractor.playAfterPrepare()
    }

    fun updateSource(source: CompositionSource, playerType: PlayerType) {
        val currentSourceInfo = preparedSourcesMap[playerType]
        if (currentSourceInfo?.source == source) {
            currentSourceInfo.source = source
        }
        if (playerType == activePlayerType) {
            playerInteractor.updateSource(source)
        }
    }

    fun playOrPause(playerType: PlayerType) {
        applyPlayerType(playerType)
        playerInteractor.playOrPause()
    }

    fun stop(playerType: PlayerType) {
        if (playerType == activePlayerType) {
            playerInteractor.stop()
        }
    }

    fun pause(playerType: PlayerType) {
        if (playerType == activePlayerType) {
            playerInteractor.pause()
        }
    }

    fun error(playerType: PlayerType, throwable: Throwable) {
        if (playerType == activePlayerType) {
            playerInteractor.error(throwable)
        }
    }

    fun reset(playerType: PlayerType) {
        preparedSourcesMap.remove(playerType)
        if (playerType == activePlayerType) {
            playerInteractor.reset()
        }
    }

    fun fastSeekForward(playerType: PlayerType): Single<Long> {
        if (playerType == activePlayerType) {
            return playerInteractor.fastSeekForward()
        }
        //do not update position if player is not active
        return getActualTrackPosition(playerType)
    }

    fun fastSeekBackward(playerType: PlayerType): Single<Long> {
        if (playerType == activePlayerType) {
            return playerInteractor.fastSeekBackward()
        }
        //do not update position if player is not active
        return getActualTrackPosition(playerType)
    }

    fun prepareToPlay(
        compositionSource: CompositionSource,
        playerType: PlayerType,
        startPosition: Long
    ) {
        preparedSourcesMap[playerType] = SourceInfo(compositionSource, startPosition)
        if (playerType == activePlayerType) {
            playerInteractor.prepareToPlay(compositionSource, startPosition)
        } else if (activePlayerType == null) {
            applyPlayerType(playerType)
        }
    }

    fun onSeekStarted(playerType: PlayerType) {
        if (activePlayerType == playerType) {
            playerInteractor.onSeekStarted()
        }
    }

    fun onSeekFinished(position: Long, playerType: PlayerType) {
        if (activePlayerType == playerType) {
            playerInteractor.onSeekFinished(position)
        } else {
            preparedSourcesMap[playerType]?.trackPosition = position
        }
    }

    fun setPlaybackSpeed(speed: Float, playerType: PlayerType) {
        if (activePlayerType == playerType) {
            playerInteractor.setPlaybackSpeed(speed)
        }
    }

    fun getPlayerEventsObservable(playerType: PlayerType): Observable<PlayerEvent> {
        return playerInteractor.getPlayerEventsObservable()
            .filter { isPlayerTypeActive(playerType) }
    }

    fun getTrackPositionObservable(playerType: PlayerType): Observable<Long> {
        return playerInteractor.getTrackPositionObservable()
            .filter { isPlayerTypeActive(playerType) }
    }

    fun getTrackPositionChangeObservable(playerType: PlayerType): Observable<Long> {
        return playerInteractor.getTrackPositionChangeObservable()
            .filter { isPlayerTypeActive(playerType) }
    }

    fun getPlayerStateObservable(playerType: PlayerType): Observable<PlayerState> {
        return playerInteractor.getPlayerStateObservable()
            .map { state -> if (isPlayerTypeActive(playerType)) state else PlayerState.IDLE }
    }

    fun getIsPlayingStateObservable(playerType: PlayerType): Observable<Boolean> {
        return playerInteractor.getIsPlayingStateObservable()
            .map { state -> if (isPlayerTypeActive(playerType)) state else false }
    }

    fun isPlayerTypeActive(playerType: PlayerType): Boolean {
        return activePlayerType == playerType
    }

    fun getSpeedChangeAvailableObservable() = playerInteractor.getSpeedChangeAvailableObservable()

    fun getActivePlayerTypeObservable(): Observable<PlayerType> {
        return activePlayerTypeSubject.flatMap { opt ->
            val value = opt.value
            return@flatMap if (value != null) {
                Observable.just(value)
            } else {
                Observable.never()
            }
        }
    }

    fun getActualTrackPosition(playerType: PlayerType): Single<Long> {
        return if (isPlayerTypeActive(playerType)) {
            playerInteractor.getTrackPosition()
        } else {
            Single.just(preparedSourcesMap[playerType]?.trackPosition ?: -1L)
        }
    }

    @Suppress("CheckResult")
    private fun applyPlayerType(playerType: PlayerType) {
        if (activePlayerType != playerType) {
            playerInteractor.pause()
            val oldSource = preparedSourcesMap[activePlayerType]
            if (oldSource != null) {
                playerInteractor.getTrackPosition()
                    .subscribe { position -> oldSource.trackPosition = position }
            }

            initializePlayerType(playerType)
            activePlayerType = playerType
            activePlayerTypeSubject.onNext(Optional(activePlayerType))

            val sourceInfo = preparedSourcesMap[playerType]
            if (sourceInfo != null) {
                playerInteractor.prepareToPlay(sourceInfo.source, sourceInfo.trackPosition)
            }
        }
    }

    private fun initializePlayerType(playerType: PlayerType) {
        when (playerType) {
            PlayerType.LIBRARY -> {
                playerInteractor.setPlaybackSpeed(uiStateRepository.currentPlaybackSpeed)
            }
            PlayerType.EXTERNAL -> {
                playerInteractor.setPlaybackSpeed(1f)
            }
        }
    }

    private inner class SourceInfo(
        var source: CompositionSource,
        var trackPosition: Long
    )

}
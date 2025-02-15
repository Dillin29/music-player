package com.github.dillin.musicplayer.domain.interactors.player;

import static com.github.dillin.musicplayer.domain.interactors.player.PlayerType.EXTERNAL;
import static com.github.dillin.musicplayer.domain.interactors.player.PlayerType.LIBRARY;

import com.github.dillin.musicplayer.domain.interactors.library.LibraryAlbumsInteractor;
import com.github.dillin.musicplayer.domain.interactors.library.LibraryArtistsInteractor;
import com.github.dillin.musicplayer.domain.interactors.library.LibraryCompositionsInteractor;
import com.github.dillin.musicplayer.domain.interactors.library.LibraryFoldersInteractor;
import com.github.dillin.musicplayer.domain.interactors.library.LibraryGenresInteractor;
import com.github.dillin.musicplayer.domain.interactors.playlists.PlayListsInteractor;
import com.github.dillin.musicplayer.domain.models.albums.Album;
import com.github.dillin.musicplayer.domain.models.albums.AlbumComposition;
import com.github.dillin.musicplayer.domain.models.artist.Artist;
import com.github.dillin.musicplayer.domain.models.composition.Composition;
import com.github.dillin.musicplayer.domain.models.folders.FileSource;
import com.github.dillin.musicplayer.domain.models.genres.Genre;
import com.github.dillin.musicplayer.domain.models.player.service.MusicNotificationSetting;
import com.github.dillin.musicplayer.domain.models.playlist.PlayList;
import com.github.dillin.musicplayer.domain.models.playlist.PlayListItem;
import com.github.dillin.musicplayer.domain.repositories.SettingsRepository;

import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public class MusicServiceInteractor {

    private final PlayerCoordinatorInteractor playerCoordinatorInteractor;
    private final LibraryPlayerInteractor libraryPlayerInteractor;
    private final ExternalPlayerInteractor externalPlayerInteractor;
    private final LibraryCompositionsInteractor libraryCompositionsInteractor;
    private final LibraryFoldersInteractor libraryFoldersInteractor;
    private final LibraryArtistsInteractor libraryArtistsInteractor;
    private final LibraryAlbumsInteractor libraryAlbumsInteractor;
    private final LibraryGenresInteractor libraryGenresInteractor;
    private final PlayListsInteractor playListsInteractor;
    private final SettingsRepository settingsRepository;

    public MusicServiceInteractor(PlayerCoordinatorInteractor playerCoordinatorInteractor,
                                  LibraryPlayerInteractor libraryPlayerInteractor,
                                  ExternalPlayerInteractor externalPlayerInteractor,
                                  LibraryCompositionsInteractor libraryCompositionsInteractor,
                                  LibraryFoldersInteractor libraryFoldersInteractor,
                                  LibraryArtistsInteractor libraryArtistsInteractor,
                                  LibraryAlbumsInteractor libraryAlbumsInteractor,
                                  LibraryGenresInteractor libraryGenresInteractor,
                                  PlayListsInteractor playListsInteractor,
                                  SettingsRepository settingsRepository) {
        this.playerCoordinatorInteractor = playerCoordinatorInteractor;
        this.libraryPlayerInteractor = libraryPlayerInteractor;
        this.externalPlayerInteractor = externalPlayerInteractor;
        this.libraryCompositionsInteractor = libraryCompositionsInteractor;
        this.libraryFoldersInteractor = libraryFoldersInteractor;
        this.libraryArtistsInteractor = libraryArtistsInteractor;
        this.libraryAlbumsInteractor = libraryAlbumsInteractor;
        this.libraryGenresInteractor = libraryGenresInteractor;
        this.playListsInteractor = playListsInteractor;
        this.settingsRepository = settingsRepository;
    }

    public void prepare() {
        if (playerCoordinatorInteractor.isPlayerTypeActive(EXTERNAL)) {
            return;
        }
        libraryPlayerInteractor.prepare();
    }

    public void play(long delay) {
        if (playerCoordinatorInteractor.isPlayerTypeActive(EXTERNAL)) {
            externalPlayerInteractor.play(delay);
        } else {
            libraryPlayerInteractor.play(delay);
        }
    }

    public void skipToNext() {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.skipToNext();
        }
    }

    public void skipToPrevious() {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.skipToPrevious();
        }
    }

    public void setRepeatMode(int appRepeatMode) {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.setRepeatMode(appRepeatMode);
        } else {
            externalPlayerInteractor.setExternalPlayerRepeatMode(appRepeatMode);
        }
    }

    public void changeRepeatMode() {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.changeRepeatMode();
        } else {
            externalPlayerInteractor.changeExternalPlayerRepeatMode();
        }
    }

    public void setRandomPlayingEnabled(boolean isEnabled) {
        libraryPlayerInteractor.setRandomPlayingEnabled(isEnabled);
    }

    public void setPlaybackSpeed(float speed) {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.setPlaybackSpeed(speed);
            return;
        }
        if (playerCoordinatorInteractor.isPlayerTypeActive(EXTERNAL)) {
            externalPlayerInteractor.setPlaybackSpeed(speed);
        }
    }

    public void fastSeekBackward() {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.fastSeekBackward();
            return;
        }
        if (playerCoordinatorInteractor.isPlayerTypeActive(EXTERNAL)) {
            externalPlayerInteractor.fastSeekBackward();
        }
    }

    public void fastSeekForward() {
        if (playerCoordinatorInteractor.isPlayerTypeActive(LIBRARY)) {
            libraryPlayerInteractor.fastSeekForward();
            return;
        }
        if (playerCoordinatorInteractor.isPlayerTypeActive(EXTERNAL)) {
            externalPlayerInteractor.fastSeekForward();
        }
    }

    public Completable shuffleAllAndPlay() {
        return libraryCompositionsInteractor.getCompositionsObservable(null)
                .firstOrError()
                .flatMapCompletable(compositions -> {
                    libraryPlayerInteractor.setRandomPlayingEnabled(true);
                    return libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions);
                });
    }

    public Completable playFromSearch(@Nullable String searchQuery) {
        return playFromSearch(searchQuery, 0);
    }

    public Completable playFromSearch(@Nullable String searchQuery, int position) {
        return libraryCompositionsInteractor.getCompositionsObservable(searchQuery)
                .firstOrError()
                .flatMapCompletable(compositions ->
                        libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions, position)
                );
    }

    public Observable<Integer> getRepeatModeObservable() {
        return playerCoordinatorInteractor.getActivePlayerTypeObservable()
                .switchMap(playerType -> {
                    switch (playerType) {
                        case LIBRARY: {
                            return libraryPlayerInteractor.getRepeatModeObservable();
                        }
                        case EXTERNAL: {
                            return externalPlayerInteractor.getExternalPlayerRepeatModeObservable();
                        }
                        default: throw new IllegalStateException();
                    }
                });
    }

    public Observable<Boolean> getRandomModeObservable() {
        return playerCoordinatorInteractor.getActivePlayerTypeObservable()
                .switchMap(playerType -> {
                    switch (playerType) {
                        case LIBRARY: {
                            return libraryPlayerInteractor.getRandomPlayingObservable();
                        }
                        case EXTERNAL: {
                            return Observable.fromCallable(() -> false);
                        }
                        default: throw new IllegalStateException();
                    }
                });
    }

    public Observable<MusicNotificationSetting> getNotificationSettingObservable() {
        return Observable.combineLatest(getCoversInNotificationEnabledObservable(),
                getColoredNotificationEnabledObservable(),
                getNotificationCoverStubEnabledObservable(),
                getCoversOnLockScreenEnabledObservable(),
                MusicNotificationSetting::new);
    }

    public Observable<List<Composition>> getCompositionsObservable(String searchText) {
        return libraryCompositionsInteractor.getCompositionsObservable(searchText);
    }

    public Completable startPlayingFromCompositions(int position) {
        return libraryCompositionsInteractor.getCompositionsObservable(null)
                .firstOrError()
                .flatMapCompletable(compositions ->
                        libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions, position)
                );
    }

    public Observable<List<FileSource>> getFoldersObservable(@Nullable Long folderId) {
        return libraryFoldersInteractor.getFoldersInFolder(folderId, null);
    }

    public Completable play(@Nullable Long folderId, long compositionId) {
        return libraryFoldersInteractor.play(folderId, compositionId);
    }

    public Observable<List<Artist>> getArtistsObservable() {
        return libraryArtistsInteractor.getArtistsObservable(null);
    }

    public Observable<List<Composition>> getCompositionsByArtist(long artistId) {
        return libraryArtistsInteractor.getCompositionsByArtist(artistId);
    }

    public Completable startPlayingFromArtistCompositions(long artistId, int position) {
        return getCompositionsByArtist(artistId)
                .firstOrError()
                .flatMapCompletable(compositions ->
                        libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions, position)
                );
    }

    public Observable<List<Album>> getAlbumsObservable() {
        return libraryAlbumsInteractor.getAlbumsObservable(null);
    }

    public Observable<List<AlbumComposition>> getAlbumItemsObservable(long albumId) {
        return libraryAlbumsInteractor.getAlbumItemsObservable(albumId);
    }

    public Completable startPlayingFromAlbumCompositions(long albumId, int position) {
        return getAlbumItemsObservable(albumId)
                .firstOrError()
                .flatMapCompletable(compositions ->
                        libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions, position)
                );
    }

    public Observable<List<Genre>> getGenresObservable() {
        return libraryGenresInteractor.getGenresObservable(null);
    }

    public Observable<List<Composition>> getGenreItemsObservable(long genreId) {
        return libraryGenresInteractor.getGenreItemsObservable(genreId);
    }

    public Completable startPlayingFromGenreCompositions(long genreId, int position) {
        return getGenreItemsObservable(genreId)
                .firstOrError()
                .flatMapCompletable(compositions ->
                        libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions, position)
                );
    }

    public Observable<List<PlayList>> getPlayListsObservable() {
        return playListsInteractor.getPlayListsObservable(null);
    }

    public Observable<List<PlayListItem>> getPlaylistItemsObservable(long playListId) {
        return playListsInteractor.getCompositionsObservable(playListId, null);
    }

    public Completable startPlayingFromPlaylistItems(long playListId, int position) {
        return getPlaylistItemsObservable(playListId)
                .firstOrError()
                .flatMapCompletable(compositions ->
                        libraryPlayerInteractor.setCompositionsQueueAndPlay(compositions, position)
                );
    }

    public MusicNotificationSetting getNotificationSettings() {
        boolean coversInNotification = isCoversInNotificationEnabled();
        boolean coloredNotification = settingsRepository.isColoredNotificationEnabled();
        boolean showNotificationCoverStub = settingsRepository.isNotificationCoverStubEnabled();
        boolean coversOnLockScreen = settingsRepository.isCoversOnLockScreenEnabled();
        return new MusicNotificationSetting(
                coversInNotification,
                coversInNotification && coloredNotification,
                coversInNotification && showNotificationCoverStub,
                coversInNotification && coversOnLockScreen
        );
    }

    public boolean isCoversInNotificationEnabled() {
        return settingsRepository.isCoversEnabled()
                && settingsRepository.isCoversInNotificationEnabled();
    }

    private Observable<Boolean> getCoversInNotificationEnabledObservable() {
        return Observable.combineLatest(settingsRepository.getCoversEnabledObservable(),
                settingsRepository.getCoversInNotificationEnabledObservable(),
                (coversEnabled, coversInNotification) -> coversEnabled && coversInNotification);
    }

    private Observable<Boolean> getColoredNotificationEnabledObservable() {
        return Observable.combineLatest(getCoversInNotificationEnabledObservable(),
                settingsRepository.getColoredNotificationEnabledObservable(),
                (coversInNotification, coloredNotification) -> coversInNotification && coloredNotification);
    }

    private Observable<Boolean> getNotificationCoverStubEnabledObservable() {
        return Observable.combineLatest(getCoversInNotificationEnabledObservable(),
                settingsRepository.getNotificationCoverStubEnabledObservable(),
                (coversInNotification, showNotificationCoverStub) -> coversInNotification && showNotificationCoverStub);
    }

    private Observable<Boolean> getCoversOnLockScreenEnabledObservable() {
        return Observable.combineLatest(getCoversInNotificationEnabledObservable(),
                settingsRepository.getCoversOnLockScreenEnabledObservable(),
                (coversInNotification, coversOnLockScreen) -> coversInNotification && coversOnLockScreen);
    }
}

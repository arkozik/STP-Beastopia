package de.uniks.beastopia.teaml.controller.ingame.scoreboard;

import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.controller.AppPreparer;
import de.uniks.beastopia.teaml.controller.ingame.UserInfoController;
import de.uniks.beastopia.teaml.controller.ingame.scoreboard.ScoreboardController;
import de.uniks.beastopia.teaml.controller.ingame.scoreboard.ScoreboardFilterController;
import de.uniks.beastopia.teaml.controller.ingame.scoreboard.ScoreboardUserItemController;
import de.uniks.beastopia.teaml.rest.Achievement;
import de.uniks.beastopia.teaml.rest.AchievementsSummary;
import de.uniks.beastopia.teaml.rest.Trainer;
import de.uniks.beastopia.teaml.rest.User;
import de.uniks.beastopia.teaml.service.AchievementsService;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.TrainerService;
import de.uniks.beastopia.teaml.utils.Prefs;
import io.reactivex.rxjava3.core.Observable;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javax.inject.Provider;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreboardControllerTest extends ApplicationTest {
    @Mock
    DataCache cache;
    @Mock
    AchievementsService achievementsService;
    @Mock
    TrainerService trainerService;
    @Mock
    Prefs prefs;
    @Mock
    Provider<ScoreboardUserItemController> scoreBoardUserItemControllerProvider;
    @Mock
    Provider<ScoreboardFilterController> scoreboardFilterControllerProvider;
    @Mock
    Provider<UserInfoController> userInfoControllerProvider;
    @InjectMocks
    ScoreboardController scoreboardController;
    private final App app = new App();
    private final List<AchievementsSummary> achievementSummaries = List.of(new AchievementsSummary("123", 0, 2, 3));
    private final List<Achievement> achievements = List.of(new Achievement(null, null, "ID2", "ID", new Date(), 0));
    private final List<User> users = List.of(new User(null, null, "ID", "Leon", "status", "avatar", null));
    private final List<Trainer> trainers = List.of(new Trainer(null, null, "TRAINER_ID", "region", "ID", "Lonartie", "image", null, List.of(), List.of(),0, "area", 0, 0, 0, null));
    private final List<Achievement> userAchievements = List.of(new Achievement(null, null, "ID", "Leon", null, 25));
    private final Pane pane = new Pane();
    private ArgumentCaptor<Consumer<String>> onUserClickedCaptor;

    @Override
    public void start(Stage stage) {
        AppPreparer.prepare(app);
        //noinspection unchecked
        onUserClickedCaptor = ArgumentCaptor.forClass(Consumer.class);

        ScoreboardUserItemController controller = mock();
        when(controller.setUser(any())).thenReturn(controller);
        when(controller.setAchievements(anyInt())).thenReturn(controller);
        when(controller.setOnUserClicked(onUserClickedCaptor.capture())).thenReturn(controller);
        when(controller.setTotalAchievements(anyInt())).thenReturn(controller);
        when(controller.render()).thenReturn(pane);
        when(controller.setUserAchievements(any())).thenReturn(controller);
        when(controller.getUserAchievements()).thenReturn(userAchievements);

        when(prefs.getRegionID()).thenReturn("region");
        when(achievementsService.getAchievements()).thenReturn(Observable.just(achievementSummaries));
        when(trainerService.getAllTrainer(any())).thenReturn(Observable.just(trainers));
        when(cache.getAllUsers()).thenReturn(users);
        when(achievementsService.getUserAchievements(any())).thenReturn(Observable.just(achievements));
        when(scoreBoardUserItemControllerProvider.get()).thenReturn(controller);

        app.start(stage);
        app.show(scoreboardController);
        stage.requestFocus();

        sleep(2000);
    }

    @Test
    void render() {
        verify(scoreBoardUserItemControllerProvider.get()).render();
    }

    @Test
    void setOnCloseRequested() {
        Runnable runnable = mock(Runnable.class);
        doNothing().when(runnable).run();
        scoreboardController.setOnCloseRequested(runnable);

        clickOn(pane);
        type(KeyCode.N);

        verify(runnable).run();
    }

    @Test
    void onUserClicked() {
        UserInfoController mocked = mock(UserInfoController.class);
        when(mocked.render()).thenReturn(new Pane());
        when(mocked.setName(any())).thenReturn(mocked);
        when(mocked.setAchievements(anyInt())).thenReturn(mocked);
        when(mocked.setTotalAchievements(anyInt())).thenReturn(mocked);
        when(mocked.setUserAchievements(any())).thenReturn(mocked);
        when(mocked.setUserAvatar(any())).thenReturn(mocked);

        when(userInfoControllerProvider.get()).thenReturn(mocked);

        sleep(2000);

        AtomicReference<Boolean> clicked = new AtomicReference<>(false);
        Platform.runLater(() -> {
            onUserClickedCaptor.getValue().accept("ID");
            clicked.set(true);
        });

        while (!clicked.get()) {
            sleep(100);
        }
        verify(userInfoControllerProvider.get()).render();
    }

    @Test
    void clickOnFilterIcon() {
        ScoreboardFilterController mocked = mock(ScoreboardFilterController.class);
        when(mocked.render()).thenReturn(new Pane());
        when(mocked.setCurrentAchievements(any())).thenReturn(mocked);
        when(mocked.setSubControllers(any())).thenReturn(mocked);
        when(mocked.setParentPane(any())).thenReturn(mocked);

        when(scoreboardFilterControllerProvider.get()).thenReturn(mocked);

        clickOn("#filterIcon");

        verify(scoreboardFilterControllerProvider.get()).render();
    }
}
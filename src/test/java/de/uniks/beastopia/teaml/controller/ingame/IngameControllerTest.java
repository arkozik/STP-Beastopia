package de.uniks.beastopia.teaml.controller.ingame;

import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.controller.AppPreparer;
import de.uniks.beastopia.teaml.controller.ingame.beastlist.BeastListController;
import de.uniks.beastopia.teaml.controller.ingame.items.ShopController;
import de.uniks.beastopia.teaml.controller.ingame.mondex.MondexListController;
import de.uniks.beastopia.teaml.controller.ingame.scoreboard.ScoreboardController;
import de.uniks.beastopia.teaml.controller.menu.PauseController;
import de.uniks.beastopia.teaml.rest.*;
import de.uniks.beastopia.teaml.service.*;
import de.uniks.beastopia.teaml.sockets.EventListener;
import de.uniks.beastopia.teaml.sockets.UDPEventListener;
import de.uniks.beastopia.teaml.utils.PlayerState;
import de.uniks.beastopia.teaml.utils.Prefs;
import de.uniks.beastopia.teaml.utils.SoundController;
import io.reactivex.rxjava3.core.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javax.inject.Provider;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngameControllerTest extends ApplicationTest {

    @Mock
    Provider<EntityController> entityControllerProvider;
    @Mock
    Provider<MapController> mapControllerProvider;
    @Mock
    Provider<SoundController> soundControllerProvider;
    @Mock
    EncounterOpponentsService encounterOpponentsService;
    @Mock
    SoundController soundController;
    @Mock
    EntityController playerController;
    @SuppressWarnings("unused")
    @Mock
    DialogWindowController dialogWindowController;
    @Mock
    AreaService areaService;
    @Mock
    TrainerService trainerService;
    @Mock
    PresetsService presetsService;
    @Mock
    AStarService aStarService;
    @Mock
    AchievementsService achievementsService;
    @Mock
    UDPEventListener udpEventListener;
    @Mock
    EventListener eventListener;
    @Mock
    BeastListController beastListController;
    @Mock
    MondexListController mondexListController;
    @Mock
    final
    PauseController pauseController = mock();
    @Mock
    DataCache cache;
    @Mock
    Prefs prefs;
    @Mock
    final
    ScoreboardController scoreboardController = mock();
    @SuppressWarnings("unused")
    @Mock
    final ShopController shopController = mock();
    @Mock
    TokenStorage tokenStorage;
    @Spy
    App app;
    @Spy
    final
    ResourceBundle resources = ResourceBundle.getBundle("de/uniks/beastopia/teaml/assets/lang");
    @InjectMocks
    IngameController ingameController;
    final TileSetDescription tileSetDescription = new TileSetDescription(0, "SOURCE");
    final TileSet tileSet = new TileSet(2, "IMAGE", 2, 2, 0, "NAME", 0, 4, 1, List.of());
    final Chunk chunk = new Chunk(List.of(0L, 1L, 2L, 3L), 2, 2, 0, 0);
    final Layer layer = new Layer(List.of(chunk), List.of(), null, null, 1, 0, 0, null, true, 2, 2, 0, 0);
    final Map map = new Map(List.of(tileSetDescription), List.of(layer), 2, 24, 4);
    final Area area = new Area(null, null, "ID_AREA", "ID_REGION", "AREA_NAME", new Position(0, 0), map);
    final Spawn spawn = new Spawn("ID_AREA", 0, 0);
    final Region region = new Region(null, null, "ID", "NAME", spawn, map);
    final Image image = createImage(2, 2, List.of(new Color(255, 0, 255), new Color(0, 255, 0), new Color(0, 0, 255), new Color(255, 255, 0)));
    final Trainer trainer = new Trainer(null, null, "ID_TRAINER", "ID_REGION", "ID_USER", "TRAINER_NAME", "TRAINER_IMAGE", null, List.of(), List.of(), 0, "ID_AREA", 0, 0, 0, new NPCInfo(false, false, false, false, List.of(), List.of(), List.of()));
    final User user = new User(null, null, "ID_USER", "USER_NAME", "USER_STATUS", "USER_AVATAR", List.of());
    final Achievement achievement = new Achievement(null, null, "MoveCharacter", "ID_USER", null, 100);
    final List<Area> areas = List.of(area);
    final MonsterTypeDto monsterTypeDto = new MonsterTypeDto(3, "name", "image", List.of("1", "2"), "a");
    final Monster monster = new Monster(null, null, "ID_MONSTER", "ID_TRAINER", 3, 1, 0, null, null, null, List.of());

    @Override
    public void start(Stage stage) {
        AppPreparer.prepare(app);

        when(tokenStorage.getCurrentUser()).thenReturn(user);
        when(trainerService.getAllTrainer(any())).thenReturn(Observable.just(List.of(trainer)));
        when(areaService.getArea(anyString(), anyString())).thenReturn(Observable.just(area));
        when(areaService.getAreas(anyString())).thenReturn(Observable.just(areas));
        when(udpEventListener.listen(anyString(), any())).thenReturn(Observable.empty());
        when(cache.getAreas()).thenReturn(List.of(area));
        doNothing().when(scoreboardController).init();
        when(eventListener.listen(any(), any())).thenReturn(Observable.empty());
        doNothing().when(prefs).setCurrentRegion(any());
        doNothing().when(prefs).setArea(any());
        when(presetsService.getTileset(tileSetDescription)).thenReturn(Observable.just(tileSet));
        when(presetsService.getImage(tileSet)).thenReturn(Observable.just(image));
        when(presetsService.getAllBeasts()).thenReturn(Observable.just(List.of(monsterTypeDto)));
        when(encounterOpponentsService.getTrainerOpponents(anyString(), anyString())).thenReturn(Observable.just(List.of()));
        when(cache.getTrainer()).thenReturn(trainer);
        when(cache.getMapImage()).thenReturn(image);
        when(cache.getMapTileset()).thenReturn(tileSet);
        when(cache.getJoinedRegion()).thenReturn(region);
        doNothing().when(cache).setTrainer(trainer);
        when(entityControllerProvider.get()).thenReturn(playerController);
        doNothing().when(playerController).setTrainer(any());
        when(playerController.playerState()).thenReturn(new SimpleObjectProperty<>(PlayerState.IDLE));
        doNothing().when(playerController).setOnTrainerUpdate(any());
        doNothing().when(playerController).init();
        when(playerController.render()).thenReturn(new Pane());
        doNothing().when(beastListController).setOnCloseRequest(any());
        doNothing().when(beastListController).setOnBeastClicked(any());
        doNothing().when(beastListController).init();
        when(beastListController.render()).thenReturn(new Pane());
        doNothing().when(mondexListController).setOnCloseRequest(any());
        doNothing().when(mondexListController).setOnBeastClicked(any());
        doNothing().when(mondexListController).init();
        when(soundControllerProvider.get()).thenReturn(soundController);
        doNothing().when(pauseController).setOnCloseRequest(any());
        doNothing().when(pauseController).init();
        when(pauseController.render()).thenReturn(new Pane());
        when(achievementsService.getUserAchievements(anyString())).thenReturn(Observable.just(List.of(achievement)));
        when(trainerService.getTrainerMonsters(anyString(), anyString())).thenReturn(Observable.just(List.of(monster)));
        when(achievementsService.updateUserAchievement(anyString(), anyString(), any())).thenReturn(Observable.just(achievement));
        ingameController.setRegion(region);

        app.start(stage);
        app.show(ingameController);
        stage.requestFocus();

        sleep(1000);
    }

    @Test
    void openMapTest() {
        final MapController mock = Mockito.mock(MapController.class);
        when(mapControllerProvider.get()).thenReturn(mock);
        when(mock.render()).thenReturn(new Label());
        type(KeyCode.M);
        verify(mock).render();
    }

    @Test
    void title() {
        assertEquals(app.getStage().getTitle(), resources.getString("titleIngame"));
    }

    @Test
    void movePlayer() {
        when(cache.getMyAchievements()).thenReturn(List.of(achievement));
        when(cache.getTrainer()).thenReturn(trainer);
        when(aStarService.isWalkable(any())).thenReturn(true);
        doNothing().when(udpEventListener).send(anyString());

        press(KeyCode.W);
        sleep(300);
        release(KeyCode.W);
        sleep(300);

        press(KeyCode.S);
        sleep(300);
        release(KeyCode.S);
        sleep(300);

        press(KeyCode.A);
        sleep(300);
        release(KeyCode.A);
        sleep(300);

        press(KeyCode.D);
        sleep(300);
        release(KeyCode.D);
        sleep(300);

        verify(udpEventListener, atLeast(4)).send(anyString());
    }

    @SuppressWarnings("SameParameterValue")
    private static Image createImage(int width, int height, List<Color> colors) {
        // create buffered image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int i = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            if (i >= colors.size()) {
                break;
            }
            for (int x = 0; x < image.getWidth(); x++) {
                if (i >= colors.size()) {
                    break;
                }
                image.setRGB(x, y, colors.get(i++).getRGB());
            }
        }
        return convertToFxImage(image);
    }

    // sauce: https://stackoverflow.com/questions/30970005/bufferedimage-to-javafx-image
    private static Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }

        return new ImageView(wr).getImage();
    }
}
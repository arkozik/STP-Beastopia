package de.uniks.beastopia.teaml.controller.ingame;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.controller.ingame.beast.EditBeastTeamController;
import de.uniks.beastopia.teaml.controller.ingame.encounter.FightWildBeastController;
import de.uniks.beastopia.teaml.controller.ingame.encounter.JoinFightInfoController;
import de.uniks.beastopia.teaml.controller.ingame.encounter.StartFightNPCController;
import de.uniks.beastopia.teaml.controller.ingame.items.InventoryController;
import de.uniks.beastopia.teaml.controller.ingame.items.ItemDetailController;
import de.uniks.beastopia.teaml.controller.ingame.items.ShopController;
import de.uniks.beastopia.teaml.controller.menu.PauseController;
import de.uniks.beastopia.teaml.rest.Achievement;
import de.uniks.beastopia.teaml.rest.Area;
import de.uniks.beastopia.teaml.rest.Chunk;
import de.uniks.beastopia.teaml.rest.Encounter;
import de.uniks.beastopia.teaml.rest.ItemTypeDto;
import de.uniks.beastopia.teaml.rest.Layer;
import de.uniks.beastopia.teaml.rest.Map;
import de.uniks.beastopia.teaml.rest.Monster;
import de.uniks.beastopia.teaml.rest.MonsterTypeDto;
import de.uniks.beastopia.teaml.rest.MoveTrainerDto;
import de.uniks.beastopia.teaml.rest.Opponent;
import de.uniks.beastopia.teaml.rest.Region;
import de.uniks.beastopia.teaml.rest.Tile;
import de.uniks.beastopia.teaml.rest.TileProperty;
import de.uniks.beastopia.teaml.rest.TileSet;
import de.uniks.beastopia.teaml.rest.TileSetDescription;
import de.uniks.beastopia.teaml.rest.Trainer;
import de.uniks.beastopia.teaml.service.AchievementsService;
import de.uniks.beastopia.teaml.service.AreaService;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.EncounterOpponentsService;
import de.uniks.beastopia.teaml.service.PresetsService;
import de.uniks.beastopia.teaml.service.RegionEncountersService;
import de.uniks.beastopia.teaml.service.TokenStorage;
import de.uniks.beastopia.teaml.service.TrainerService;
import de.uniks.beastopia.teaml.sockets.EventListener;
import de.uniks.beastopia.teaml.sockets.UDPEventListener;
import de.uniks.beastopia.teaml.utils.Dialog;
import de.uniks.beastopia.teaml.utils.Direction;
import de.uniks.beastopia.teaml.utils.LoadingPage;
import de.uniks.beastopia.teaml.utils.PlayerState;
import de.uniks.beastopia.teaml.utils.Prefs;
import de.uniks.beastopia.teaml.utils.SoundController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import static de.uniks.beastopia.teaml.service.PresetsService.PREVIEW_SCALING;

public class IngameController extends Controller {
    static final double TILE_SIZE = 48;
    static final int MENU_NONE = 0;
    static final int MENU_SCOREBOARD = 1;
    static final int MENU_BEASTLIST = 2;
    static final int MENU_PAUSE = 3;
    static final int MENU_SHOP = 4;
    static final int MENU_INVENTORY = 5;
    static final int MENU_DIALOGWINDOW = 3;
    static final long FLIPPED_HORIZONTALLY_FLAG = 1L << 31;
    static final long FLIPPED_VERTICALLY_FLAG = 1L << 30;
    static final long FLIPPED_DIAGONALLY_FLAG = 1L << 29;
    static final long ROTATED_HEXAGONAL_120_FLAG = 1L << 28;

    @FXML
    public Pane tilePane;
    @FXML
    private HBox scoreBoardLayout;
    @FXML
    private StackPane pauseMenuLayout;
    @FXML
    private Button pauseHint;
    @FXML
    private Button beastlistHint;
    @FXML
    private Button scoreboardHint;
    @FXML
    private Button mapHint;
    @FXML
    private Button invHint;
    @FXML
    private HBox shopLayout;
    @Inject
    App app;
    @Inject
    AreaService areaService;
    @Inject
    PresetsService presetsService;
    @Inject
    TrainerService trainerService;
    @Inject
    AchievementsService achievementsService;
    @Inject
    PauseController pauseController;
    @Inject
    Provider<StartFightNPCController> startFightNPCControllerProvider;
    @Inject
    BeastListController beastListController;
    @Inject
    ShopController shopController;
    @Inject
    InventoryController inventoryController;
    @Inject
    Provider<BeastDetailController> beastDetailControllerProvider;
    @Inject
    Provider<DialogWindowController> dialogWindowControllerProvider;
    @Inject
    Provider<EditBeastTeamController> editBeastTeamControllerProvider;
    @Inject
    Provider<EntityController> entityControllerProvider;
    @Inject
    Provider<MapController> mapControllerProvider;
    @Inject
    Provider<JoinFightInfoController> joinFightInfoControllerProvider;
    @Inject
    Provider<ItemDetailController> itemDetailControllerProvider;
    @Inject
    Prefs prefs;
    @Inject
    DataCache cache;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    UDPEventListener udpEventListener;
    @Inject
    EventListener eventListener;
    @Inject
    ScoreboardController scoreBoardController;
    @Inject
    Provider<IngameController> ingameControllerProvider;
    @Inject
    Provider<SoundController> soundControllerProvider;
    @Inject
    Provider<FightWildBeastController> fightWildBeastControllerProvider;
    @Inject
    RegionEncountersService regionEncountersService;
    @Inject
    EncounterOpponentsService encounterOpponentsService;
    private Region region;
    private Map map;
    private final List<Pair<TileSetDescription, Pair<TileSet, Image>>> tileSets = new ArrayList<>();
    private int posx = 0;
    private int posy = 0;
    private int lastposx = 0;
    private int lastposy = 0;
    private boolean spawned = false;
    private int width;
    private int height;
    private LoadingPage loadingPage;
    private final List<Controller> subControllers = new ArrayList<>();
    private Monster lastMonster;
    private ItemTypeDto lastItemTypeDto;
    private int currentMenu = MENU_NONE;
    private final java.util.Map<Pair<Integer, Integer>, Tile> MAP_INFO = new HashMap<>();
    Direction direction;
    final ObjectProperty<PlayerState> state = new SimpleObjectProperty<>();
    Parent player;
    Parent beastListParent;
    Parent beastDetailParent;
    Parent itemDetailParent;
    EntityController playerController;
    SoundController soundController;
    Parent scoreBoardParent;
    Parent pauseMenuParent;
    Parent dialogWindowParent;
    Parent shopParent;
    Parent inventoryParent;
    final java.util.Map<EntityController, Parent> otherPlayers = new HashMap<>();
    private final List<KeyCode> pressedKeys = new ArrayList<>();
    private final String[] locationStrings = {"Moncenter", "House", "Store"};
    private long lastValueChangeTime = 0;
    private DialogWindowController dialogWindowController;
    private Timer timer;

    @Inject
    public IngameController() {
    }

    /**
     * Initializes the controller by Creating the main player and assigning its callback functions
     */
    @Override
    public void init() {
        super.init();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                moveLoop();
            }
        }, 0, 100);

        if (cache.getMapImage() == null || cache.getMapTileset() == null) {
            cache.setTileset(presetsService.getTileset(cache.getJoinedRegion().map().tilesets().get(0)).blockingFirst());
            cache.setMapImage(presetsService.getImage(cache.getMapTileset()).blockingFirst());
        }

        scoreBoardController.setOnCloseRequested(() -> {
            scoreBoardLayout.getChildren().remove(scoreBoardParent);
            currentMenu = MENU_NONE;
        });
        scoreBoardController.init();

        beastListController.setOnCloseRequest(() -> {
            scoreBoardLayout.getChildren().remove(beastListParent);
            scoreBoardLayout.getChildren().remove(beastDetailParent);
            lastMonster = null;
            currentMenu = MENU_NONE;
        });
        beastListController.setOnBeastClicked(this::toggleBeastDetails);
        beastListController.init();

        pauseController.setOnCloseRequest(() -> {
            pauseMenuLayout.getChildren().remove(pauseMenuParent);
            currentMenu = MENU_NONE;
        });
        pauseController.init();

        state.setValue(PlayerState.IDLE);
        playerController = entityControllerProvider.get();
        playerController.playerState().bind(state);
        playerController.setOnTrainerUpdate(trainer -> {
            Trainer myTrainer = cache.getTrainer();
            List<String> visited = new ArrayList<>(myTrainer.visitedAreas());

            if (!visited.contains(trainer.area())) {
                visited.add(trainer.area());
            }

            Trainer updatedTrainer = new Trainer(
                    myTrainer.createdAt(),
                    myTrainer.updatedAt(),
                    myTrainer._id(),
                    myTrainer.region(),
                    myTrainer.user(),
                    myTrainer.name(),
                    myTrainer.image(),
                    myTrainer.team(),
                    visited,
                    myTrainer.coins(),
                    trainer.area(),
                    trainer.x(),
                    trainer.y(),
                    trainer.direction(),
                    myTrainer.npc()
            );

            cache.setTrainer(updatedTrainer);

            if (!trainer.area().equals(prefs.getArea()._id())) {
                if (Arrays.stream(locationStrings).anyMatch(cache.getArea(trainer.area()).name()::contains)) {
                    soundController.play("sfx:opendoor");
                }

                IngameController controller = ingameControllerProvider.get();
                controller.setRegion(region);
                controller.checkAreaAchievement(cache.getArea(trainer.area())._id());
                app.show(controller);
                return;
            }
            posx = trainer.x();
            posy = trainer.y();
            updateOrigin();

            spawned = true;
        });

        soundController = soundControllerProvider.get();
    }

    /**
     * Sets the region this controller should load into
     *
     * @param region The region of the current trainer
     */
    public void setRegion(Region region) {
        prefs.setCurrentRegion(region);
        this.region = region;
    }

    @Override
    public Parent render() {
        loadingPage = LoadingPage.makeLoadingPage(super.render());

        currentMenu = MENU_NONE;

        disposables.add(trainerService.getAllTrainer(this.region._id())
                .subscribe(this::loadTrainers));

        return loadingPage.parent();
    }

    private void loadTrainers(List<Trainer> trainers) {
        Trainer myTrainer = loadMyTrainer(trainers);
        cache.setTrainer(myTrainer);
        cache.setTrainers(trainers);

        disposables.add(areaService.getAreas(this.region._id()).observeOn(FX_SCHEDULER).subscribe(areas -> {
            cache.setAreas(areas);
            loadMap(cache.getAreas(), myTrainer, trainers);
        }));

        disposables.add(eventListener.listen("encounters.*.trainers." + cache.getTrainer()._id()
                        + ".opponents.*.created", Opponent.class)
                .observeOn(FX_SCHEDULER)
                .concatMap(opponentEvent -> {
                    Opponent opponent = opponentEvent.data();
                    return regionEncountersService.getRegionEncounter(cache.getJoinedRegion()._id(), opponent.encounter())
                            .observeOn(FX_SCHEDULER);
                })
                .subscribe(
                        encounter -> {
                            cache.setCurrentEncounter(encounter);

                            if (encounter.isWild()) {
                                openFightBeastScreen(encounter);
                            } else {
                                openFightNPCScreen(encounter);
                            }
                        },
                        error -> System.err.println("Fehler: " + error.getMessage())
                )
        );

        pauseHint.toFront();
        beastlistHint.toFront();
        scoreboardHint.toFront();
        mapHint.toFront();
        invHint.toFront();
    }

    private void openFightNPCScreen(Encounter encounter) {
        StartFightNPCController controller = startFightNPCControllerProvider.get();
        controller.setControllerInfo(encounter);
        app.show(controller);
    }

    private void openFightBeastScreen(Encounter encounter) {
        FightWildBeastController controller = fightWildBeastControllerProvider.get();
        disposables.add(encounterOpponentsService
                .getEncounterOpponents(cache.getJoinedRegion()._id(), encounter._id())
                .observeOn(FX_SCHEDULER)
                .subscribe(o -> {
                    for (Opponent op : o) {
                        if (op.trainer().equals("000000000000000000000000")) {
                            controller.setControllerInfo(op.monster(), op.trainer());
                            app.show(controller);
                        }
                    }
                }));
    }

    private void listenToTrainerEvents() {
        disposables.add(eventListener.listen(
                        "regions." + this.region._id() + ".trainers.*.created",
                        Trainer.class)
                .observeOn(FX_SCHEDULER)
                .subscribe(event -> this.createRemotePlayer(event.data()),
                        error -> Dialog.error(error, resources.getString("getAllTrainerError"))));
        disposables.add(eventListener.listen(
                        "regions." + this.region._id() + ".trainers.*.deleted",
                        Trainer.class)
                .observeOn(FX_SCHEDULER)
                .subscribe(event -> this.removeRemotePlayer(event.data()),
                        error -> Dialog.error(error, resources.getString("getAllTrainerError"))));
    }

    private void loadRemoteTrainer(List<Trainer> trainers) {
        for (Trainer trainer : trainers) {
            if (trainer._id().equals(cache.getTrainer()._id())) {
                continue;
            }
            createRemotePlayer(trainer);
        }


        disposables.add(udpEventListener.listen("areas.*.trainers.*.moved", MoveTrainerDto.class).observeOn(FX_SCHEDULER).subscribe(event -> {
            MoveTrainerDto dto = event.data();
            if (dto == null) {
                return;
            }

            if (cache.getTrainer()._id().equals(dto._id())) {
                playerController.updateTrainer(dto);
                return;
            }

            for (EntityController entityController : otherPlayers.keySet()) {
                if (entityController.getTrainer()._id().equals(dto._id())) {
                    entityController.updateTrainer(dto);
                    return;
                }
            }
        }));
    }

    private void loadMap(List<Area> areas, Trainer myTrainer, List<Trainer> trainers) {
        Area area = areas.stream().filter(a -> a._id().equals(myTrainer.area())).findFirst().orElseThrow();
        prefs.setArea(area);

        disposables.add(areaService.getArea(this.region._id(), area._id())
                .observeOn(FX_SCHEDULER)
                .subscribe(a -> {
                            this.map = a.map();
                            for (TileSetDescription tileSetDesc : map.tilesets()) {
                                TileSet tileSet = presetsService.getTileset(tileSetDesc).blockingFirst();
                                Image image = presetsService.getImage(tileSet).blockingFirst();
                                tileSets.add(new Pair<>(tileSetDesc, new Pair<>(tileSet, image)));
                            }
                            drawMap();

                            if (a.name().contains("Route")) {
                                soundController.play("bgm:route");
                            } else if (a.name().contains("House")) {
                                soundController.play("bgm:house");
                            } else {
                                soundController.play("bgm:city");
                            }

                            beastListParent = beastListController.render();
                            pauseMenuParent = pauseController.render();
                            loadRemoteTrainer(trainers);
                            listenToTrainerEvents();
                            loadingPage.setDone();
                        }
                ));
    }

    /**
     * Looks for the current trainer in the list and initializes the player controller
     *
     * @param trainers The list of all trainers of the current region
     * @return Our current trainer
     */
    private Trainer loadMyTrainer(List<Trainer> trainers) {
        Trainer myTrainer = trainers.stream().filter(t -> t.user().equals(tokenStorage.getCurrentUser()._id())).findFirst().orElseThrow();

        playerController.setTrainer(myTrainer);
        playerController.init();

        cache.setTrainer(myTrainer);
        posx = myTrainer.x();
        posy = myTrainer.y();
        playerController.setDirection(myTrainer.direction());

        return myTrainer;
    }

    private void createRemotePlayer(Trainer trainer) {
        EntityController controller = entityControllerProvider.get();
        ObjectProperty<PlayerState> ps = new SimpleObjectProperty<>();
        controller.playerState().bind(ps);
        ps.setValue(PlayerState.IDLE);
        controller.setTrainer(trainer);
        controller.setOnTrainerUpdate(moveDto -> {
            if (!isBeingRendered(moveDto._id()) && moveDto.area().equals(prefs.getArea()._id())) {
                revealRemotePlayer(cache.getTrainer(moveDto._id()));
            } else if (isBeingRendered(moveDto._id()) && !moveDto.area().equals(prefs.getArea()._id())) {
                hideRemotePlayer(cache.getTrainer(moveDto._id()));
            } else {
                moveRemotePlayer(controller, moveDto.x(), moveDto.y());
            }
        });
        controller.init();
        controller.setDirection(trainer.direction());
        Parent parent = drawRemotePlayer(controller, trainer.x(), trainer.y());
        otherPlayers.put(controller, parent);
        if (prefs.getArea() != null && !prefs.getArea()._id().equals(trainer.area())) {
            hideRemotePlayer(trainer);
        }

        if (trainer.area().equals(cache.getTrainer().area()) && trainer.npc() == null) {
            disposables.add(eventListener.listen("encounters.*.trainers." + trainer._id()
                            + ".opponents.*.created", Opponent.class)
                    .observeOn(FX_SCHEDULER)
                    .subscribe(o -> checkOpponents(Collections.singletonList(o.data()), trainer),
                            error -> System.err.println("Fehler: " + error.getMessage())
                    )
            );
        }
    }

    private void checkOpponents(List<Opponent> opponents, Trainer trainer) {
        if (trainer.area().equals(cache.getTrainer().area())) {
            disposables.add(encounterOpponentsService.getEncounterOpponents(cache.getJoinedRegion()._id(), opponents.get(0).encounter())
                    .observeOn(FX_SCHEDULER)
                    .subscribe(o -> {
                        if (o.size() == 3) {
                            for (Opponent opponent : o) {
                                if (opponent.trainer().equals(trainer._id()) && !opponent.isAttacker()) {
                                    // This is to get the current position of the trainer, because it gives the wrong
                                    // coordinates when using "trainer.x()" and "trainer.y()" (trainer creation coordinates?)
                                    disposables.add(trainerService.getTrainer(cache.getJoinedRegion()._id(), trainer._id())
                                            .observeOn(FX_SCHEDULER)
                                            .subscribe(t -> {
                                                        JoinFightInfoController joinFightInfoController = joinFightInfoControllerProvider.get();
                                                        joinFightInfoController.setX(t.x() * TILE_SIZE);
                                                        joinFightInfoController.setY(t.y() * TILE_SIZE);
                                                        joinFightInfoController.setParent(tilePane);
                                                        joinFightInfoController.init();

                                                        Parent parentView = joinFightInfoController.render();
                                                        tilePane.getChildren().add(parentView);
                                                    }
                                            ));
                                    break;
                                }
                            }
                        }
                    }));
        }
    }

    private EntityController getEntityController(Trainer trainer) {
        EntityController trainerController = null;
        for (EntityController controller : otherPlayers.keySet()) {
            if (controller.getTrainer()._id().equals(trainer._id())) {
                trainerController = controller;
                break;
            }
        }
        return trainerController;
    }

    private void removeRemotePlayer(Trainer trainer) {
        EntityController trainerController = getEntityController(trainer);
        if (trainerController == null) {
            return;
        }
        tilePane.getChildren().remove(otherPlayers.get(trainerController));
        trainerController.destroy();
        otherPlayers.remove(trainerController);
    }

    private void hideRemotePlayer(Trainer trainer) {
        if (trainer == null) {
            return;
        }
        EntityController trainerController = getEntityController(trainer);
        if (trainerController == null) {
            return;
        }
        tilePane.getChildren().remove(otherPlayers.get(trainerController));
    }

    private void revealRemotePlayer(Trainer trainer) {
        if (trainer == null) {
            return;
        }
        EntityController trainerController = getEntityController(trainer);
        if (trainerController == null) {
            return;
        }
        tilePane.getChildren().add(otherPlayers.get(trainerController));
    }

    private boolean isBeingRendered(String trainer) {
        for (EntityController controller : otherPlayers.keySet()) {
            if (controller.getTrainer()._id().equals(trainer)) {
                return tilePane.getChildren().contains(otherPlayers.get(controller));
            }
        }
        return false;
    }

    private void drawMap() {
        drawPlayer(posx, posy);
        for (Layer layer : map.layers()) {
            if (layer.chunks() != null) {
                for (Chunk chunk : layer.chunks()) {
                    layTiles(chunk.x(), chunk.y(), chunk.data(), chunk.width());
                }
            } else if (layer.data() != null) {
                layTiles(layer.x(), layer.y(), layer.data(), layer.width());
            }
        }

        updateOrigin();
    }

    private void layTiles(int chunkX, int chunkY, List<Long> data, int width) {
        int index = 0;
        for (long id : data) {
            int x = index % width + chunkX;
            int y = index / width + chunkY;
            index++;
            long localID = id & ~(FLIPPED_HORIZONTALLY_FLAG | FLIPPED_VERTICALLY_FLAG | FLIPPED_DIAGONALLY_FLAG | ROTATED_HEXAGONAL_120_FLAG);
            Pair<Pair<TileSet, Image>, Long> tileSet = findTileSet(localID);
            if (tileSet == null) {
                continue;
            }

            // Some maps have "invalid" (or blank tiles) with ID 0 which we don't want to draw
            // This is to prevent the camera from showing the "extended" tile pane with those tiles
            if (id != 0) {
                List<Tile> tileInformation = tileSet.getKey().getKey().tiles();
                tileInformation.stream().filter(t -> t.id() == tileSet.getValue()).findFirst().ifPresent(tile -> MAP_INFO.put(new Pair<>(x, y), tile));
                drawTile(id, x, y, tileSet.getKey().getValue(), presetsService.getTileViewPort(tileSet.getValue(), tileSet.getKey().getKey()));
            }
        }
    }

    private Pair<Pair<TileSet, Image>, Long> findTileSet(long id) {
        id++;
        for (int i = tileSets.size() - 1; i >= 0; i--) {
            Pair<TileSetDescription, Pair<TileSet, Image>> tileSet = tileSets.get(i);
            if (tileSet.getKey().firstgid() <= id) {
                return new Pair<>(tileSet.getValue(), id - tileSet.getKey().firstgid());
            }
        }
        return null;
    }

    private void drawTile(long ID, int x, int y, Image image, Rectangle2D viewPort) {
        boolean flippedHorizontally = (ID & FLIPPED_HORIZONTALLY_FLAG) != 0;
        boolean flippedVertically = (ID & FLIPPED_VERTICALLY_FLAG) != 0;
        boolean flippedDiagonally = (ID & FLIPPED_DIAGONALLY_FLAG) != 0;

        ImageView view = new ImageView();
        view.setSmooth(true);
        view.setFitWidth(TILE_SIZE + 1);
        view.setFitHeight(TILE_SIZE + 1);
        view.setViewport(viewPort);
        view.setTranslateX(x * TILE_SIZE);
        view.setTranslateY(y * TILE_SIZE);
        view.setImage(image);

        if (flippedDiagonally) {
            view.setScaleX(-1);
            view.setRotate(90);
        }

        if (flippedHorizontally) {
            view.setScaleX(-1);
        }

        if (flippedVertically) {
            view.setScaleY(-1);
        }

        tilePane.getChildren().add(view);
    }

    public void setOrigin(int tilex, int tiley) {
        double originX = (double) width / 2 - TILE_SIZE / 2;
        double originY = (double) height / 2 - TILE_SIZE / 2;
        double tilePaneTranslationX = originX - tilex * TILE_SIZE;
        double tilePaneTranslationY = originY - tiley * TILE_SIZE;

        // Calculate the maximum translation values based on the map dimensions and visible area
        double maxTranslationX = Math.max(0, tilePane.getBoundsInLocal().getWidth() - width + (TILE_SIZE / 2));
        double maxTranslationY = Math.max(0, tilePane.getBoundsInLocal().getHeight() - height + TILE_SIZE);

        // Clamp the translation values within the maximum range
        tilePaneTranslationX = Math.max(-maxTranslationX, Math.min(0, tilePaneTranslationX));
        tilePaneTranslationY = Math.max(-maxTranslationY, Math.min(0, tilePaneTranslationY));

        tilePane.setTranslateX(tilePaneTranslationX);
        tilePane.setTranslateY(tilePaneTranslationY);
        movePlayer(tilex, tiley);
        prefs.setPosition(new Point2D(tilex, tiley));

        if (spawned) {
            long currentTime = System.currentTimeMillis();
            // Delay in milliseconds
            double debounceDelay = 250;
            if (currentTime - lastValueChangeTime > debounceDelay) {
                if (lastposx == posx && lastposy == posy) {
                    soundController.play("sfx:bump");
                    lastValueChangeTime = currentTime;
                }
            }
        }
    }

    public void updateOrigin() {
        if (tilePane != null) {
            setOrigin(posx, posy);
        }
    }

    private void drawPlayer(int posx, int posy) {
        tilePane.getChildren().remove(player);
        player = playerController.render();
        player.setTranslateX(posx * TILE_SIZE);
        player.setTranslateY((posy - 1) * TILE_SIZE);
        tilePane.getChildren().add(player);
        player.toFront();
    }

    private Parent drawRemotePlayer(EntityController controller, int posx, int posy) {
        Parent parent = controller.render();
        parent.setTranslateX(posx * TILE_SIZE);
        parent.setTranslateY((posy - 1) * TILE_SIZE);
        tilePane.getChildren().add(parent);
        parent.toFront();
        return parent;
    }

    private void movePlayer(int x, int y) {
        playerController.updateViewPort();
        player.toFront();
        player.setTranslateX(x * TILE_SIZE);
        player.setTranslateY((y - 1) * TILE_SIZE);
    }

    private void moveRemotePlayer(EntityController controller, int x, int y) {
        Parent remotePlayer = otherPlayers.get(controller);
        controller.updateViewPort();
        remotePlayer.toFront();
        remotePlayer.setTranslateX(x * TILE_SIZE);
        remotePlayer.setTranslateY((y - 1) * TILE_SIZE);
    }

    @Override
    public void onResize(int width, int height) {
        this.width = width;
        this.height = height;
        if (loadingPage.isDone()) {
            updateOrigin();
        }
    }

    private void toggleBeastDetails(Monster monster) {
        if (Objects.equals(lastMonster, monster)) {
            scoreBoardLayout.getChildren().remove(beastDetailParent);
            lastMonster = null;
            return;
        }

        lastMonster = monster;

        BeastDetailController controller = beastDetailControllerProvider.get();
        subControllers.add(controller);
        controller.setBeast(monster);
        controller.init();

        scoreBoardLayout.getChildren().remove(beastDetailParent);
        beastDetailParent = controller.render();
        scoreBoardLayout.getChildren().add(0, beastDetailParent);
    }

    private void updateTrainerPos(Direction direction) {
        Trainer trainer = cache.getTrainer();
        JsonObject data = new JsonObject();
        data.add("_id", new JsonPrimitive(trainer._id()));
        data.add("area", new JsonPrimitive(trainer.area()));
        data.add("x", new JsonPrimitive(posx));
        data.add("y", new JsonPrimitive(posy));
        data.add("direction", new JsonPrimitive(direction.ordinal()));

        Pair<Integer, Integer> posXY = new Pair<>(posx, posy);
        if (MAP_INFO.containsKey(posXY)) {
            Tile tile = MAP_INFO.get(posXY);
            Optional<TileProperty> jumpableTileProp = tile.properties().stream().filter(tileProperty -> tileProperty.name().equals("Jumpable")).findFirst();
            if (jumpableTileProp.isPresent()) {
                int jumpDirection = Integer.parseInt(jumpableTileProp.get().value());
                if (jumpDirection == direction.ordinal()) {
                    state.setValue(PlayerState.JUMP);
                    drawPlayer(posx, posy);
                    movePlayer(posx, posy);
                    updateOrigin();
                }
            }
        }

        JsonObject message = new JsonObject();
        message.add("event", new JsonPrimitive("areas." + trainer.area() + ".trainers." + trainer._id() + ".moved"));
        message.add("data", data);

        udpEventListener.send(message.toString());
    }

    @FXML
    public void keyDown(KeyEvent keyEvent) {
        handlePlayerMovement(keyEvent);
        handleMap(keyEvent);
        handlePauseMenu(keyEvent);
        handleScoreboard(keyEvent);
        handleBeastList(keyEvent);
        handleInventory(keyEvent);
        handleBeastTeam(keyEvent);
        handleTalkToTrainer(keyEvent);
    }

    public void handleTalkToTrainer(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.T)) {
            Trainer trainer = canTalkToNPC();
            if (trainer != null) {
                if (trainer.npc() == null || trainer.npc().encounterOnTalk()) {
                    List<Opponent> trainerOpponents = encounterOpponentsService.getTrainerOpponents(cache.getJoinedRegion()._id(), trainer._id()).blockingFirst();

                    if (!(trainerOpponents.equals(List.of()))) {
                        List<Opponent> allOpponents = encounterOpponentsService.getEncounterOpponents(cache.getJoinedRegion()._id(), trainerOpponents.get(0).encounter()).blockingFirst();

                        if (allOpponents.size() == 3) {
                            talkToJoinFight(trainer);
                        } else {
                            talkToFightingNPC(trainer);
                        }
                    } else {
                        startEncounterOnTalk(trainer);
                    }
                } else if (trainer.npc().starters() != null) {
                    talkToStartersNPC(trainer);
                } else if (trainer.npc().canHeal()) {
                    talkToNurse(trainer);
                } else if (trainer.npc().sells() != null) {
                    openShop(trainer);
                }
            } else {
                closeTalk();
            }
        }
    }

    private Trainer canTalkToNPC() {
        for (Trainer trainer : cache.getTrainers()) {
            if (trainer.area().equals(cache.getTrainer().area())) {
                if (direction == Direction.RIGHT) { // right
                    if (trainer.x() == posx + 1 && trainer.y() == posy) {
                        return trainer;
                    }
                } else if (direction == Direction.UP) { //up
                    if (trainer.x() == posx && trainer.y() == posy - 1) {
                        return trainer;
                    }
                } else if (direction == Direction.LEFT) { //left
                    if (trainer.x() == posx - 1 && trainer.y() == posy) {
                        return trainer;
                    }
                } else if (direction == Direction.DOWN) { //down
                    if (trainer.x() == posx && trainer.y() == posy + 1) {
                        return trainer;
                    }
                }
            }
        }
        for (Trainer trainer : cache.getTrainers()) {
            if (trainer.area().equals(cache.getTrainer().area())) {
                if (direction == Direction.RIGHT) { // right
                    if (trainer.x() == posx + 2 && trainer.y() == posy && (trainer.npc().canHeal() || trainer.npc().sells() != null)) {
                        return trainer;
                    }
                } else if (direction == Direction.UP) { //up
                    if (trainer.x() == posx && trainer.y() == posy - 2 && (trainer.npc().canHeal() || trainer.npc().sells() != null)) {
                        return trainer;
                    }
                } else if (direction == Direction.LEFT) { //left
                    if (trainer.x() == posx - 2 && trainer.y() == posy && (trainer.npc().canHeal() || trainer.npc().sells() != null)) {
                        return trainer;
                    }
                } else if (direction == Direction.DOWN) { //down
                    if (trainer.x() == posx + 2 && trainer.y() == posy + 2 && (trainer.npc().canHeal() || trainer.npc().sells() != null)) {
                        return trainer;
                    }
                }
            }
        }
        return null;
    }

    private String createTalkMessage(String _id, String target, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Integer> selection) {
        JsonObject data = new JsonObject();
        data.add("_id", new JsonPrimitive(_id));
        data.add("target", new JsonPrimitive(target));

        if (selection.isPresent()) {
            data.add("selection", new JsonPrimitive(selection.get()));
        } else {
            data.add("selection", JsonNull.INSTANCE);
        }

        JsonObject eventMessage = new JsonObject();
        eventMessage.add("event", new JsonPrimitive("areas." + cache.getTrainer().area() + ".trainers." + cache.getTrainer()._id() + ".talked"));
        eventMessage.add("data", data);

        return eventMessage.toString();
    }

    private void startEncounterOnTalk(Trainer trainer) {
        disposables.add(cache.getOrLoadTrainerImage(trainer.image(), false)
                .observeOn(FX_SCHEDULER)
                .subscribe(image -> {
                    Rectangle2D viewPort = new Rectangle2D(48 * PREVIEW_SCALING, 0, 16 * PREVIEW_SCALING, 32 * PREVIEW_SCALING);
                    PixelReader reader = image.getPixelReader();
                    WritableImage newImage = new WritableImage(reader, (int) viewPort.getMinX(), (int) viewPort.getMinY(), (int) viewPort.getWidth(), (int) viewPort.getHeight());
                    String askFight = resources.getString("ask") + " " + trainer.name() + " " + resources.getString("vs");
                    talk(newImage, resources.getString("hello") + " \n" + resources.getString("nurse"), List.of(askFight), null,
                            i -> udpEventListener.send(createTalkMessage(cache.getTrainer()._id(), trainer._id(), Optional.empty())));
                }));
    }

    private void talkToJoinFight(Trainer trainer) {
        disposables.add(cache.getOrLoadTrainerImage(trainer.image(), false)
                .observeOn(FX_SCHEDULER)
                .subscribe(image -> {
                    Rectangle2D viewPort = new Rectangle2D(48 * PREVIEW_SCALING, 0, 16 * PREVIEW_SCALING, 32 * PREVIEW_SCALING);
                    PixelReader reader = image.getPixelReader();
                    WritableImage newImage = new WritableImage(reader, (int) viewPort.getMinX(), (int) viewPort.getMinY(), (int) viewPort.getWidth(), (int) viewPort.getHeight());
                    String askJoinFight = "Join the fight of " + trainer.name();
                    talk(newImage, resources.getString("hello") + " \n" + "You can try to join the fight!", List.of(askJoinFight), null,
                            i -> udpEventListener.send(createTalkMessage(cache.getTrainer()._id(), trainer._id(), Optional.empty())));
                }));
    }

    private void talkToFightingNPC(Trainer trainer) {
        disposables.add(cache.getOrLoadTrainerImage(trainer.image(), false)
                .observeOn(FX_SCHEDULER)
                .subscribe(image -> {
                    Rectangle2D viewPort = new Rectangle2D(48 * PREVIEW_SCALING, 0, 16 * PREVIEW_SCALING, 32 * PREVIEW_SCALING);
                    PixelReader reader = image.getPixelReader();
                    WritableImage newImage = new WritableImage(reader, (int) viewPort.getMinX(), (int) viewPort.getMinY(), (int) viewPort.getWidth(), (int) viewPort.getHeight());
                    talk(newImage, "I am currently fighting, come back later!"
                            , null, null, null);
                }));
    }

    private void talkToStartersNPC(Trainer trainer) {
        disposables.add(cache.getOrLoadTrainerImage(trainer.image(), false)
                .observeOn(FX_SCHEDULER)
                .subscribe(image -> {
                    Rectangle2D viewPort = new Rectangle2D(48 * PREVIEW_SCALING, 0, 16 * PREVIEW_SCALING, 32 * PREVIEW_SCALING);
                    PixelReader reader = image.getPixelReader();
                    WritableImage newImage = new WritableImage(reader, (int) viewPort.getMinX(), (int) viewPort.getMinY(), (int) viewPort.getWidth(), (int) viewPort.getHeight());
                    if (trainer.npc().encountered().contains(cache.getTrainer()._id())) {
                        talk(newImage, " Welcome back! \n You already received one starter Beast, have a nice day! "
                                , null, null, null);
                    } else {
                        List<String> beastNames = new ArrayList<>();
                        List<Image> beastImages = new ArrayList<>();
                        List<MonsterTypeDto> monsterTypeDtoList = new ArrayList<>();
                        for (int id : trainer.npc().starters()) {
                            MonsterTypeDto monsterTypeDto = presetsService.getMonsterType(id).blockingFirst();
                            beastNames.add(monsterTypeDto.name());
                            monsterTypeDtoList.add(monsterTypeDto);
                            Image beastImage = presetsService.getMonsterImage(id).blockingFirst();
                            beastImages.add(beastImage);
                        }
                        talk(newImage, " Welcome! \n Please select a starter Beast. ", beastNames, beastImages, (i -> {
                            String message = "Details: " + monsterTypeDtoList.get(i).name() + "\n" + monsterTypeDtoList.get(i).description();
                            talk(newImage, message, List.of("Take it!", "I don't want this one"), null, (j -> {
                                if (j == 0) {
                                    udpEventListener.send(createTalkMessage(cache.getTrainer()._id(), trainer._id(), Optional.of(i)));
                                    closeTalk();
                                } else {
                                    talkToStartersNPC(trainer);
                                }
                            }));
                        }));
                    }
                }));
    }

    private void talkToNurse(Trainer trainer) {
        disposables.add(cache.getOrLoadTrainerImage(trainer.image(), false)
                .observeOn(FX_SCHEDULER)
                .subscribe(image -> {
                    Rectangle2D viewPort = new Rectangle2D(48 * PREVIEW_SCALING, 0, 16 * PREVIEW_SCALING, 32 * PREVIEW_SCALING);
                    PixelReader reader = image.getPixelReader();
                    WritableImage newImage = new WritableImage(reader, (int) viewPort.getMinX(), (int) viewPort.getMinY(), (int) viewPort.getWidth(), (int) viewPort.getHeight());
                    talk(newImage, " Hello! \n what can I do for you? ", List.of("Heal all Beasts"), null, (i -> {
                        udpEventListener.send(createTalkMessage(cache.getTrainer()._id(), trainer._id(), Optional.empty()));
                        closeTalk();
                    }));
                }));
    }

    private void talk(Image image, String message, List<String> choices, List<Image> buttonImages, Consumer<Integer> onButtonPressed) {
        closeTalk();

        dialogWindowController = dialogWindowControllerProvider.get();
        dialogWindowController
                .setTrainerImage(image)
                .setChoices(choices)
                .setButtonImages(buttonImages)
                .setText(message)
                .setOnButtonClicked(onButtonPressed);
        dialogWindowParent = dialogWindowController.render();
        pauseMenuLayout.getChildren().add(dialogWindowParent);
        pauseMenuLayout.setPrefWidth(600);
        currentMenu = MENU_DIALOGWINDOW;

        dialogWindowController.setOnCloseRequested(this::closeTalk);
    }

    private void closeTalk() {
        if (pauseMenuLayout.getChildren().contains(dialogWindowParent)) {
            pauseMenuLayout.getChildren().remove(dialogWindowParent);
            currentMenu = MENU_NONE;
            if (dialogWindowController != null) {
                dialogWindowController.destroy();
            }
            player.setOpacity(1);
        }
    }

    public void handleBeastList(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.B) && (currentMenu == MENU_NONE || currentMenu == MENU_BEASTLIST)) {
            openBeastlist();
        }
    }

    public void handleInventory(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.I) && (currentMenu == MENU_NONE || currentMenu == MENU_INVENTORY)) {
            openInventory(false);
        }
    }

    private void handleScoreboard(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.N) && (currentMenu == MENU_NONE || currentMenu == MENU_SCOREBOARD)) {
            openScoreboard();
        }
    }

    private void handlePauseMenu(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ESCAPE) && (currentMenu == MENU_NONE || currentMenu == MENU_PAUSE)) {
            openPauseMenu();
        }
    }

    private void handleMap(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.M)) {
            MapController map = mapControllerProvider.get();
            app.show(map);
        }
    }

    private void handleBeastTeam(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.X)) {
            app.show(editBeastTeamControllerProvider.get());
        }
    }


    private void handlePlayerMovement(KeyEvent keyEvent) {
        if (!pressedKeys.contains(keyEvent.getCode())) {
            if (keyEvent.getCode().equals(KeyCode.UP) || keyEvent.getCode().equals(KeyCode.W)) {
                pressedKeys.add(keyEvent.getCode());
            } else if (keyEvent.getCode().equals(KeyCode.DOWN) || keyEvent.getCode().equals(KeyCode.S)) {
                pressedKeys.add(keyEvent.getCode());
            } else if (keyEvent.getCode().equals(KeyCode.LEFT) || keyEvent.getCode().equals(KeyCode.A)) {
                pressedKeys.add(keyEvent.getCode());
            } else if (keyEvent.getCode().equals(KeyCode.RIGHT) || keyEvent.getCode().equals(KeyCode.D)) {
                pressedKeys.add(keyEvent.getCode());
            }
        }
    }

    public void keyUp(KeyEvent keyEvent) {
        pressedKeys.removeIf(keyCode -> keyCode.equals(keyEvent.getCode()));
        setIdleState();
    }

    public void moveLoop() {
        if (currentMenu == MENU_NONE) {
            boolean moved = false;

            lastposx = posx;
            lastposy = posy;

            if (pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W)) {
                posy--;
                direction = Direction.UP;
                moved = true;
            } else if (pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S)) {
                posy++;
                direction = Direction.DOWN;
                moved = true;
            } else if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) {
                posx--;
                direction = Direction.LEFT;
                moved = true;
            } else if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) {
                posx++;
                direction = Direction.RIGHT;
                moved = true;
            }

            if (moved) {
                checkMovementAchievement();

                onUI(() -> {
                    state.setValue(PlayerState.WALKING);
                    updateTrainerPos(direction);
                    updateOrigin();
                });
            }
        }
    }

    @FXML
    public void setIdleState() {
        state.setValue(PlayerState.IDLE);
        drawPlayer(posx, posy);

        if (currentMenu == MENU_PAUSE || currentMenu == MENU_SHOP) {
            player.setOpacity(0.5);
        }
    }

    private void checkMovementAchievement() {
        Achievement firstMovementAchievement = cache.getMyAchievements().stream()
                .filter(achievement -> achievement.id().equals("MoveCharacter"))
                .findFirst()
                .orElse(null);

        if (firstMovementAchievement == null) {
            firstMovementAchievement = new Achievement(null, null, "MoveCharacter", tokenStorage.getCurrentUser()._id(), new Date(), 100);
            cache.addMyAchievement(firstMovementAchievement);

            disposables.add(achievementsService.updateUserAchievement(tokenStorage.getCurrentUser()._id(), "MoveCharacter", firstMovementAchievement).observeOn(FX_SCHEDULER)
                    .subscribe(a -> Dialog.info(resources.getString("achievementUnlockHeader"), resources.getString("achievementUnlockedPre") + "\n" + resources.getString("achievementMoveCharacter"))));
        }
    }

    private void checkAreaAchievement(String areaId) {
        String areasString;
        String foundArea = "";
        String areaIdSub = areaId.substring(areaId.length() - 6);

        for (String visitedArea : cache.getVisitedAreas()) {
            if (areaIdSub.equals(visitedArea)) {
                foundArea = visitedArea;
                break;
            }
        }

        if (foundArea.isEmpty()) {
            cache.addVisitedArea(areaIdSub);
            areasString = String.join(";", cache.getVisitedAreas());
            prefs.addVisitedArea(areasString);
            double percentage = (double) cache.getVisitedAreas().size() / cache.getAreas().size() * 100;

            Achievement allAreasAchievement = cache.getMyAchievements().stream()
                    .filter(achievement -> achievement.id().equals("VisitAllRegions"))
                    .findFirst()
                    .orElse(null);

            Date date = new Date();

            if (allAreasAchievement == null) {
                allAreasAchievement = new Achievement(date, null, "VisitAllRegions", tokenStorage.getCurrentUser()._id(), null, (int) Math.round(percentage));
                cache.addMyAchievement(allAreasAchievement);

                disposables.add(achievementsService.updateUserAchievement(tokenStorage.getCurrentUser()._id(), "VisitAllRegions", allAreasAchievement).subscribe());
            } else {
                if (allAreasAchievement.unlockedAt() == null) {
                    Achievement updatedAchievement = new Achievement(null, date, "VisitAllRegions", tokenStorage.getCurrentUser()._id(), allAreasAchievement.progress() == 100.0 ? date : null, (int) Math.round(percentage));
                    cache.getMyAchievements().remove(allAreasAchievement);
                    cache.addMyAchievement(updatedAchievement);

                    disposables.add(achievementsService.updateUserAchievement(tokenStorage.getCurrentUser()._id(), "VisitAllRegions", updatedAchievement).subscribe());

                    if (percentage == 100.0) {
                        Dialog.info(resources.getString("achievementUnlockHeader"), resources.getString("achievementUnlockedPre") + "\n" + resources.getString("achievementVisitAllRegions"));
                    }
                }
            }
        }
    }

    @FXML
    public void clickOnMapButton() {
        MapController map = mapControllerProvider.get();
        app.show(map);
    }

    @FXML
    public void clickOnBeastListButton() {
        openBeastlist();
    }

    @FXML
    public void clickOnScoreboardButton() {
        openScoreboard();
    }

    @FXML
    public void clickOnPauseMenuButton() {
        openPauseMenu();
    }

    @FXML
    void clickOnInventoryButton() {
        openInventory(false);
    }

    public void openBeastlist() {
        if (scoreBoardLayout.getChildren().contains(beastListParent)) {
            scoreBoardLayout.getChildren().remove(beastListParent);
            scoreBoardLayout.getChildren().remove(beastDetailParent);
            lastMonster = null;
            currentMenu = MENU_NONE;
        } else {
            scoreBoardLayout.getChildren().add(beastListParent);
            currentMenu = MENU_BEASTLIST;
        }
    }

    public void openScoreboard() {
        if (scoreBoardParent != null && scoreBoardLayout.getChildren().contains(scoreBoardParent)) {
            scoreBoardLayout.getChildren().remove(scoreBoardParent);
            scoreBoardController.destroy();
            scoreBoardParent = null;
            currentMenu = MENU_NONE;
        } else {
            scoreBoardParent = scoreBoardController.render();
            scoreBoardLayout.getChildren().add(scoreBoardParent);
            currentMenu = MENU_SCOREBOARD;
        }
    }

    public void openInventory(boolean isShop) {
        if (!isShop) {
            openInventory();
        } else {
            openShopInventory();
        }
    }

    public void openInventory() {
        if (scoreBoardLayout.getChildren().contains(inventoryParent)) {
            scoreBoardLayout.getChildren().remove(inventoryParent);
            currentMenu = MENU_NONE;
        } else {
            currentMenu = MENU_INVENTORY;
            if (scoreBoardLayout.getChildren().contains(scoreBoardLayout)) {
                scoreBoardLayout.getChildren().remove(scoreBoardParent);
                currentMenu = MENU_NONE;
            } else if (scoreBoardLayout.getChildren().contains(beastListParent)) {
                scoreBoardLayout.getChildren().remove(beastListParent);
                currentMenu = MENU_NONE;
            } else {
                inventoryController.init();
                inventoryController.setIfShop(false);
                inventoryController.setOnItemClicked(this::toggleInventoryItemDetails);
                inventoryController.setOnCloseRequest(() -> {
                    setCloseRequests(scoreBoardLayout, inventoryParent);
                    lastMonster = null;
                    setCloseRequests(scoreBoardLayout, itemDetailParent);
                });
                inventoryParent = inventoryController.render();
                scoreBoardLayout.getChildren().add(inventoryParent);
            }
        }
    }

    public void setOpacities(int value) {
        pauseHint.setOpacity(value);
        pauseHint.setDisable(value == 0);
        beastlistHint.setOpacity(value);
        beastlistHint.setDisable(value == 0);
        scoreboardHint.setOpacity(value);
        scoreboardHint.setDisable(value == 0);
        mapHint.setOpacity(value);
        mapHint.setDisable(value == 0);
        invHint.setOpacity(value);
        invHint.setDisable(value == 0);
    }

    public void openShopInventory() {
        setOpacities(0);
        for (Node tile : tilePane.getChildren()) {
            if (tile instanceof ImageView imageView) {
                imageView.setFitWidth(TILE_SIZE);
                imageView.setFitHeight(TILE_SIZE);
            }
            tile.setOpacity(0.5);
        }
        if (scoreBoardLayout.getChildren().contains(scoreBoardLayout)) {
            scoreBoardLayout.getChildren().remove(scoreBoardParent);
            currentMenu = MENU_NONE;
        } else if (scoreBoardLayout.getChildren().contains(beastListParent)) {
            scoreBoardLayout.getChildren().remove(beastListParent);
            currentMenu = MENU_NONE;
        } else if (scoreBoardLayout.getChildren().contains(inventoryParent)) {
            scoreBoardLayout.getChildren().remove(inventoryParent);
            currentMenu = MENU_NONE;
        } else {
            inventoryController.init();
            inventoryController.setIfShop(true);
            inventoryController.setOnItemClicked(this::toggleInventoryItemDetails);
            inventoryController.setOnCloseRequest(() -> {
                setCloseRequests(scoreBoardLayout, inventoryParent);
                lastMonster = null;
                setCloseRequests(scoreBoardLayout, itemDetailParent);
                inventoryController.destroy();
            });
            inventoryParent = inventoryController.render();
            scoreBoardLayout.getChildren().add(inventoryParent);
        }
    }

    public void setCloseRequests(HBox hBox, Parent parent) {
        closePause();
        hBox.getChildren().remove(parent);
        currentMenu = MENU_NONE;
        setOpacities(1);
    }

    public void openShop(Trainer trainer) {
        shopController.init();
        shopController.setTrainer(trainer);
        for (Node tile : tilePane.getChildren()) {
            if (tile instanceof ImageView imageView) {
                imageView.setFitWidth(TILE_SIZE);
                imageView.setFitHeight(TILE_SIZE);
            }
            tile.setOpacity(0.5);
        }
        setOpacities(0);
        shopController.setOnItemClicked(this::toggleShopItemDetails);
        shopController.setOnCloseRequest(() -> {
            setCloseRequests(shopLayout, shopParent);
            inventoryController.close();
            inventoryController.destroy();
            setCloseRequests(shopLayout, itemDetailParent);
            shopController.destroy();
        });
        shopParent = shopController.render();
        shopLayout.getChildren().add(shopParent);
        currentMenu = MENU_SHOP;
        openInventory(true);
    }

    private void toggleInventoryItemDetails(ItemTypeDto itemTypeDto) {
        if (Objects.equals(lastItemTypeDto, itemTypeDto)) {
            scoreBoardLayout.getChildren().remove(itemDetailParent);
            lastItemTypeDto = null;
            return;
        }
        setItemDetailController(itemTypeDto, false);
    }

    private void toggleShopItemDetails(ItemTypeDto itemTypeDto) {
        if (Objects.equals(lastItemTypeDto, itemTypeDto)) {
            shopLayout.getChildren().remove(itemDetailParent);
            lastItemTypeDto = null;
            return;
        }
        setItemDetailController(itemTypeDto, true);
    }

    private void setItemDetailController(ItemTypeDto itemTypeDto, boolean booleanShop) {
        lastItemTypeDto = itemTypeDto;
        ItemDetailController controller = itemDetailControllerProvider.get();
        subControllers.add(controller);
        controller.setItem(itemTypeDto);
        controller.setBooleanShop(booleanShop);
        controller.init();
        scoreBoardLayout.getChildren().remove(itemDetailParent);
        shopLayout.getChildren().remove(itemDetailParent);
        itemDetailParent = controller.render();
        if (booleanShop) {
            shopLayout.getChildren().add(1, itemDetailParent);
        } else {
            scoreBoardLayout.getChildren().add(0, itemDetailParent);
        }
    }

    public void openPauseMenu() {
        if (pauseMenuLayout.getChildren().contains(pauseMenuParent)) {
            closePause();
            pauseMenuLayout.getChildren().remove(pauseMenuParent);
            currentMenu = MENU_NONE;
        } else {
            for (Node tile : tilePane.getChildren()) {
                if (tile instanceof ImageView imageView) {
                    imageView.setFitWidth(TILE_SIZE);
                    imageView.setFitHeight(TILE_SIZE);
                }
                tile.setOpacity(0.5);
            }
            pauseHint.setOpacity(0);
            pauseMenuLayout.getChildren().add(pauseMenuParent);
            currentMenu = MENU_PAUSE;
        }
    }

    public void closePause() {
        for (Node tile : tilePane.getChildren()) {
            if (tile instanceof ImageView imageView) {
                imageView.setFitWidth(TILE_SIZE + 1);
                imageView.setFitHeight(TILE_SIZE + 1);
            }
            tile.setOpacity(1);
        }
        pauseHint.setOpacity(1);
    }

    @Override
    public String getTitle() {
        return resources.getString("titleIngame");
    }

    @Override
    public void destroy() {
        timer.cancel();
        playerController.destroy();
        scoreBoardController.destroy();
        beastListController.destroy();
        shopController.destroy();

        if (dialogWindowController != null) {
            dialogWindowController.destroy();
        }

        for (Controller controller : subControllers) {
            controller.destroy();
        }
        subControllers.clear();

        for (EntityController controller : otherPlayers.keySet()) {
            controller.destroy();
        }
        otherPlayers.clear();

        tilePane.getChildren().clear();
        loadingPage = null;
        player = null;
        beastListParent = null;
        beastDetailParent = null;
        playerController = null;
        soundController = null;
        scoreBoardParent = null;
        pauseMenuParent = null;
        dialogWindowParent = null;
        tilePane = null;
        super.destroy();
    }
}
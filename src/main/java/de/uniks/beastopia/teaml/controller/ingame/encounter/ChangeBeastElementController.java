package de.uniks.beastopia.teaml.controller.ingame.encounter;

import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.Monster;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.PresetsService;
import de.uniks.beastopia.teaml.utils.AssetProvider;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;

public class ChangeBeastElementController extends Controller {
    @FXML
    public ImageView beastImg;
    @FXML
    public Label beastLabel;
    @FXML
    public ProgressBar expProgress;
    @FXML
    public ProgressBar hpBar;
    @FXML
    public GridPane changeBeastElement;
    @FXML
    public Button addOrRemoveButton;

    @Inject
    PresetsService presetsService;
    @Inject
    DataCache cache;
    @Inject
    AssetProvider assets;
    @Inject
    Provider<ChangeBeastController> changeBeastControllerProvider;
    ChangeBeastController changeBeastController;

    private Monster monster;
    private VBox teamPane;
    private VBox fightingPane;
    private ImageView removeImage;
    private ImageView addImage;

    @Inject
    public ChangeBeastElementController() {
    }

    public ChangeBeastElementController setMonster(Monster monster) {
        this.monster = monster;
        return this;
    }

    public ChangeBeastElementController setParentController(ChangeBeastController controller) {
        this.changeBeastController = controller;
        return this;
    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        removeImage = assets.getIcon("buttons", "minus", 20, 20);
        addImage = assets.getIcon("buttons", "plus", 20, 20);

        beastLabel.setText(cache.getBeastDto(monster.type()).name() + " " + cache.getBeastDto(monster.type()).type() + " Lv. " + monster.level());
        beastLabel.setStyle("-fx-font-size: 16px");
        if (!cache.imageIsDownloaded(monster.type())) {
            Image monsterImage = presetsService.getMonsterImage(monster.type()).blockingFirst();
            cache.addMonsterImages(monster.type(), monsterImage);
            beastImg.setImage(cache.getMonsterImage(monster.type()));
        } else {
            beastImg.setImage(cache.getMonsterImage(monster.type()));
        }

        int maxExp = (int) Math.round(Math.pow(monster.level(), 3) - Math.pow((monster.level() - 1), 3));
        expProgress.setProgress((double) monster.experience() / maxExp);
        hpBar.setProgress(monster.currentAttributes().health() / monster.attributes().health());

        teamPane = changeBeastController.beastTeam;
        fightingPane = changeBeastController.currentBeasts;

        if (changeBeastController.getBankMonsters().contains(monster)) {
            addOrRemoveButton.setGraphic(addImage);
        } else {
            addOrRemoveButton.setGraphic(removeImage);
        }

        return parent;
    }

    @FXML
    public void addOrRemoveBeast() {
        if (changeBeastController.getBankMonsters().contains(monster)) {
            changeBeastController.getBankMonsters().remove(monster);
            changeBeastController.getFightingMonsters().add(monster);

            teamPane.getChildren().remove(changeBeastElement);
            fightingPane.getChildren().add(changeBeastElement);
            addOrRemoveButton.setGraphic(removeImage);
        } else {
            changeBeastController.getBankMonsters().add(monster);
            changeBeastController.getFightingMonsters().remove(monster);

            fightingPane.getChildren().remove(changeBeastElement);
            teamPane.getChildren().add(changeBeastElement);
            addOrRemoveButton.setGraphic(addImage);
        }
    }
}

package de.uniks.beastopia.teaml.controller.ingame.encounter;

import de.uniks.beastopia.teaml.Main;
import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.AbilityDto;
import de.uniks.beastopia.teaml.rest.Monster;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.PresetsService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class LevelUpController extends Controller {
    @FXML
    public Label headline;
    @FXML
    public ImageView image;
    @FXML
    public Label up_text;
    @FXML
    public Label attack;
    @FXML
    public Label type;
    @FXML
    public Label accuracy;
    @FXML
    public Label power;
    @FXML
    public Label up_text_bottom;
    @FXML
    public Label lifeValueLabel;
    @FXML
    public Label maxLifeLabel;
    @FXML
    public Label plusHPLabel;
    @FXML
    public Label xpValueLabel;
    @FXML
    public Label maxXpLabel;
    @FXML
    public Button continueBtn;
    @FXML
    public Label abilityLabel;
    @FXML
    public ImageView heart;
    @FXML
    public ImageView star;
    @FXML
    public HBox hpBg;
    @FXML
    public HBox starBg;
    @FXML
    public HBox borderBg;
    @FXML
    public VBox beastInfo;
    @FXML
    public VBox abilityInfo;
    @FXML
    public Label statsLabel;
    @Inject
    PresetsService presetsService;
    @Inject
    DataCache cache;
    private Monster beast;
    private Map<String, Integer> newAbilities;
    private boolean dev;
    private double plusHP;
    private int plusAttack;
    private int plusDefense;
    private int plusSpeed;
    private double healthWidth;
    private double expWidth;
    private EndScreenController endScreenController;

    @Inject
    public LevelUpController() {
    }

    public void setBeast(Monster beast, Map<String, Integer> newAbilities, boolean dev, double hp, int attack, int defense, int speed, EndScreenController endScreenController) {
        this.newAbilities = newAbilities;
        this.beast = beast;
        this.dev = dev;
        this.plusHP = hp;
        this.plusAttack = attack;
        this.plusDefense = defense;
        this.plusSpeed = speed;
        this.endScreenController = endScreenController;
    }

    @Override
    public String getTitle() {
        return resources.getString("titleEncounter");
    }

    @Override
    public void onResize(int width, int height) {
        hpBg.setMinWidth(healthWidth * borderBg.getWidth());
        hpBg.setMaxWidth(healthWidth * borderBg.getWidth());
        starBg.setMinWidth(expWidth * borderBg.getWidth());
        starBg.setMaxWidth(expWidth * borderBg.getWidth());
    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        abilityInfo.setVisible(false);
        headline.setText("Level up!");
        heart.setImage(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("assets/herz.png"))));
        heart.setFitWidth(25);
        heart.setFitHeight(25);

        star.setImage(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("assets/star.png"))));
        star.setFitWidth(25);
        star.setFitHeight(25);

        up_text_bottom.setText(cache.getBeastDto(beast.type()).name() + " " + cache.getBeastDto(beast.type()).type() + " Lvl. " + beast.level());
        if (this.newAbilities != null) {
            up_text.setText(cache.getBeastDto(beast.type()).name() + " " + resources.getString("lvl+A") + " ");
        } else {
            up_text.setText(cache.getBeastDto(beast.type()).name() + " " + resources.getString("lvl+"));
        }

        lifeValueLabel.setText((int) beast.currentAttributes().health() + " ");
        maxLifeLabel.setText(" " + (int) beast.attributes().health());
        plusHPLabel.setText(" (+" + (int) plusHP + " Max HP)");
        statsLabel.setText("Attack: " + beast.attributes().attack() + " (+" + plusAttack + "), Defense: " +
                beast.attributes().defense() + " (+" + plusDefense + "), Speed: " + beast.attributes().speed() + " (+" + plusSpeed + ")");
        xpValueLabel.setText(beast.experience() + " ");
        int maxExp = (int) Math.pow(beast.level(), 3) - (int) Math.pow(beast.level() - 1, 3);
        maxXpLabel.setText(maxExp + " ");

        healthWidth = beast.currentAttributes().health() / beast.attributes().health();
        expWidth = (double) beast.experience() / maxExp;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    hpBg.setMinWidth(healthWidth * borderBg.getWidth());
                    hpBg.setMaxWidth(healthWidth * borderBg.getWidth());
                    starBg.setMinWidth(expWidth * borderBg.getWidth());
                    starBg.setMaxWidth(expWidth * borderBg.getWidth());
                });
            }
        }, 200);

        if (dev) { //Fade old Image -> New one
            image.setImage(cache.getMonsterImage(beast.type() - 1));

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), image);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.play();

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), image);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            fadeOut.setOnFinished(event -> { // set new img and start fade
                if (cache.imageIsDownloaded(beast.type())) {
                    image.setImage(cache.getMonsterImage(beast.type()));
                    fadeIn.play();
                } else {
                    disposables.add(presetsService.getMonsterImage(beast.type())
                            .observeOn(FX_SCHEDULER)
                            .subscribe(afterImage -> {
                                cache.addMonsterImages(beast.type(), afterImage);
                                image.setImage(afterImage);
                                fadeIn.play();
                            }));
                }
            });
        } else {
            image.setImage(cache.getMonsterImage(beast.type()));
        }

        if (this.newAbilities != null) {
            Map.Entry<String, Integer> lastEntry = null;
            for (Map.Entry<String, Integer> entry : newAbilities.entrySet()) {
                lastEntry = entry;
            }
            if (lastEntry != null) {
                abilityInfo.setVisible(true);

                int index = Integer.parseInt(lastEntry.getKey());
                if (cache.getAbilities().containsKey(index)) {
                    AbilityDto abilityDto = cache.getAbilities().get(index);
                    abilityLabel.setText(cache.getAbilities().get(index).name() + ".");
                    attack.setText(abilityDto.name());
                    accuracy.setText("Accuracy: " + abilityDto.accuracy());
                    type.setText("Type: " + abilityDto.type());
                    power.setText("Power: " + abilityDto.power());
                } else {
                    disposables.add(presetsService.getAbility(Integer.parseInt(lastEntry.getKey()))
                            .observeOn(FX_SCHEDULER)
                            .subscribe(abilityDto -> {
                                cache.getAbilities().put(abilityDto.id(), abilityDto);
                                abilityLabel.setText(abilityDto.name() + ".");
                                attack.setText(abilityDto.name());
                                accuracy.setText("Accuracy: " + abilityDto.accuracy());
                                type.setText("Type: " + abilityDto.type());
                                power.setText("Power: " + abilityDto.power());
                            }));
                }
            }
        }

        return parent;
    }


    @FXML
    public void continuePressed() {
        app.show(endScreenController);
    }
}

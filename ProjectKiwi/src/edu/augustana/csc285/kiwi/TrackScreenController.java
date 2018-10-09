package edu.augustana.csc285.kiwi;

import javafx.application.Platform; 

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import autotracking.AutoTrackListener;
import autotracking.AutoTracker;
import project.AnimalTrack;
import project.ProjectData;
import project.TimePoint;
import project.Video;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import project.AnimalTrack;
import project.Video;
import utils.UtilsForOpenCV;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import autotracking.AutoTrackListener;
import autotracking.AutoTracker;

import org.opencv.imgproc.Imgproc;

public class TrackScreenController implements AutoTrackListener {

	private VideoCapture capture = new VideoCapture();
	private int startFrameNum;
	private String filePath = "";
	@FXML private ImageView videoView;
	@FXML private Slider sliderSeekBar;
	@FXML private Button browseButton;
	@FXML private Button submitButton;
	@FXML private Button backwardBtn;
	@FXML private Button forwardBtn;
	@FXML private BorderPane videoPane;
	@FXML private ChoiceBox<String> chickChoice;
	@FXML private AnchorPane trackPane;
	@FXML private Label timeLabel;
	@FXML private ChoiceBox<Integer> timeStepCb;

	private List<Circle> currentDots = new ArrayList<>();
	private Color[] color = new Color[] { Color.PURPLE, Color.AQUA, Color.YELLOW };
	private int[] timeStep = new int[] { 1, 3, 5,10 };

	private AutoTracker autotracker;
	private ProjectData project;
	private Stage stage;
	private ScheduledExecutorService timer;
	public ArrayList<String> chickNames = new ArrayList<String>();

	@FXML
	public void initialize() {
		for (int i = 0; i < timeStep.length ; i++) {
			timeStepCb.getItems().add(timeStep[i]);
		}
		
		sliderSeekBar.valueProperty().addListener((obs, oldV, newV) -> showFrameAt(newV.intValue())); 
		

	}
	public void showFrameAt(int frameNum) {
		if (autotracker == null || !autotracker.isRunning()) {
			project.getVideo().setCurrentFrameNum(frameNum);
			Image curFrame = UtilsForOpenCV.matToJavaFXImage(project.getVideo().readFrame());
			videoView.setImage(curFrame);
		
			
			
		}		
	}
	@FXML
	public void handleBackward() {
		videoPane.getChildren().removeAll(currentDots);
		int time = timeStep[timeStepCb.getSelectionModel().getSelectedIndex()];
		// can we call the change to seconds method in the Video class?
		double curFrameNum = getClearFrameNum() - (30*time);
		capture.set(Videoio.CAP_PROP_POS_FRAMES, curFrameNum);
		setFrameNum(getClearFrameNum() - 30*time);
		setTimeLabel(curFrameNum);
		showFrameAt((int)curFrameNum);
		sliderSeekBar.setValue((int)curFrameNum);
	}
	@FXML
	public void handleForward() {
		videoPane.getChildren().removeAll(currentDots);
		int time = timeStep[timeStepCb.getSelectionModel().getSelectedIndex()];
		// can we call the change to seconds method in the Video class?
		double curFrameNum = getClearFrameNum() + (30*time);
		capture.set(Videoio.CAP_PROP_POS_FRAMES, curFrameNum);
		setFrameNum(getClearFrameNum() + (30*time));
		setTimeLabel(curFrameNum);
		showFrameAt((int)curFrameNum);
		sliderSeekBar.setValue((int)curFrameNum);
	}
	
	public void drawDot(MouseEvent event) {
		try {
			Circle dot = new Circle();
			dot.setCenterX(event.getX() + videoView.getLayoutX());
			dot.setCenterY(event.getY() + videoView.getLayoutY());
			dot.setRadius(5);
			dot.setFill(color[chickChoice.getSelectionModel().getSelectedIndex()+1]);
			currentDots.add(dot);
			// add circle to scene
			videoPane.getChildren().add(dot);
		} catch (Exception e) {
			System.err.println("Choose a chick");

		}
		chickChoice.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {

		});

		// project.getTracks().add(chick1);
		// project.getTracks().add(chick2);
		// project.getTracks().add(chick3);
	}

	public void setChickNames(ArrayList<String> chickName) {
		this.chickNames = chickName;
		for (int i = 0; i < chickNames.size(); i++) {
			chickChoice.getItems().add(chickNames.get(i));
		}

	}

	public void initializeAfterSceneCreated() {
		videoView.fitWidthProperty().bind(videoView.getScene().widthProperty());

	}

	@FXML
	public void handleBrowse()  {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Video File");
		File chosenFile = fileChooser.showOpenDialog(stage);
		if (chosenFile != null) {
			loadVideo(chosenFile.getPath());
		}	
		AnimalTrack chick1 = new AnimalTrack(chickNames.get(0));
		AnimalTrack chick2 = new AnimalTrack(chickNames.get(1));
		AnimalTrack chick3 = new AnimalTrack(chickNames.get(2));

		project.getTracks().add(0, chick1);
		project.getTracks().add(1, chick2);
		project.getTracks().add(2, chick3);
	
	}
	
	public void loadVideo(String filePath) {
		try {
			project = new ProjectData(filePath);
			Video video = project.getVideo();
			sliderSeekBar.setMax(video.getTotalNumFrames()-1);
			showFrameAt(0);
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}

	}
	//how do we update the label as the tracking happens. 
	@FXML
	public void handleAutoTracking() {
		if (autotracker == null || !autotracker.isRunning()) {
			project.getVideo().setXPixelsPerCm(5.5);
			project.getVideo().setYPixelsPerCm(5.5);
			project.getVideo().setStartFrameNum(startFrameNum);
			autotracker = new AutoTracker();
			// Use Observer Pattern to give autotracker a reference to this object,
			// and call back to methods in this class to update progress.
			autotracker.addAutoTrackListener(this);
			// this method will start a new thread to run AutoTracker in the background
			// so that we don't freeze up the main JavaFX UI thread.
			autotracker.startAnalysis(project.getVideo());
			submitButton.setText("CANCEL auto-tracking");
		} else {
			autotracker.cancelAnalysis();
			submitButton.setText("Start auto-tracking");
		}

	}

	// this method will get called repeatedly by the Autotracker after it analyzes
	// each frame
	@Override
	public void handleTrackedFrame(Mat frame, int frameNumber, double fractionComplete) {
		Image imgFrame = UtilsForOpenCV.matToJavaFXImage(frame);
		// this method is being run by the AutoTracker's thread, so we must
		// ask the JavaFX UI thread to update some visual properties
		Platform.runLater(() -> {
			videoView.setImage(imgFrame);
			// progressAutoTrack.setProgress(fractionComplete);
			sliderSeekBar.setValue(frameNumber);
			// textFieldCurFrameNum.setText(String.format("%05d",frameNumber));
		});
	}

	@Override
	public void trackingComplete(List<AnimalTrack> trackedSegments) {
		project.getUnassignedSegments().clear();
		project.getUnassignedSegments().addAll(trackedSegments);

		for (AnimalTrack track : trackedSegments) {
			System.out.println(track);
		}
		Platform.runLater(() -> {
			// progressAutoTrack.setProgress(1.0);
			submitButton.setText("Start auto-tracking");
		});

	}
	// Maybe get Empty frame from Video class?
	public double getClearFrameNum() {
		return startFrameNum;
	}
	//same thing for this
	public void setFrameNum(double clearFrameNum) {
		this.startFrameNum = (int) clearFrameNum;
	}

	public void setTimeLabel(double curFrameNum) {
		int minute = (int) (curFrameNum / 30) / 60;
		int second = (int) (curFrameNum / 30) - minute * 60;
		String time = "";
		if (second < 10) {
			time = "0" + minute + ":" + "0" + second;
		} else {
			time = "0" + minute + ":" + second;
		}
		timeLabel.setText(time);
	}


}

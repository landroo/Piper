package org.landroo.piper;

/*
This is a simple pipe line builder game.
The goal to let reach the fluid all ends by turn the pipes in correct position.
- You can resize the pipes and the size of the playground.
- You can scroll and zoom the playground by two fingers.
- You can set the color of pipe and the fluid. 

v 1.0
 
Ez egy egyszerű csővezeték építő játék.
A cél elérni a folyadékkal az összes csővéget a magfelelő csődarabok elforgatásával.
- A csövek és a pálya átméretezhető.
- A pálya görgethető és nagyítható.
- A csövek színe és a folyadék színe beállítható.
 
v 1.0
 */

// TODO
// pipe counter after finish	ok
// small display ratios			ok
// player list update			ok
// on back to start screen		ok
// under playing save the state
// fluid on start screen		ok
// pipe counter 				ok
// back from settings screen	ok
// unfinished fluids
// zoom on game end				ok
// scroll out zoomed table

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class PiperActivity extends Activity implements UIInterface
{
	private static final String TAG = "PiperActivity";
	private static final int SWIPE_INTERVAL = 10;

	private UI ui = null;

	private int displayWidth = 0; // display width
	private int displayHeight = 0; // display height

	private PipeView piperView; // the view

	private static Bitmap back = null; // the paper
	private BitmapDrawable backDrawable;

	private float sX = 0;
	private float sY = 0;
	private float mX = 0;
	private float mY = 0;

	private float xPos = 0;
	private float yPos = 0;
	
	private float xOffPos = 0;
	private float yOffPos = 0;

	public float tableWidth;
	public float tableHeight;
	public float origWidth;
	public float origHeight;

	private Timer swipeTimer = null;
	private float swipeDistX = 0;
	private float swipeDistY = 0;
	private float swipeVelocity = 0;
	private float swipeSpeed = 0;
	private float backSpeedX = 0;
	private float backSpeedY = 0;
	private float offMarginX = 0;
	private float offMarginY = 0;

	private float zoomSize = 0;
	private float zoomX = 1;
	private float zoomY = 1;

	private PiperClass piper;
	private int pipeSize = 80;
	private int pipeDiv = 8;

	private boolean run = false;

	private int mode = 0;

	private float tileSpeedX = 0;
	private float tileSpeedY = 0;

	private float xMul = 1;
	private float yMul = 1;

	private int scrollIn = 0;
	private boolean newGame = true;
	private boolean bFirst = true;

	private int pipeColor = 0xFFFFFFFF;
	private int fluidColor = 0xFF0000FF;
	private int shadowColor = 0xFF000000;
	private String playerName;
	private String[] playerArr = new String[5];
	private String playerList = "Landroo\t(8x14)\t100;Amoba\t(8x14)\t90;Colorizer\t(8x14)\t80;Jewels\t(8x14)\t70;tetxReader\t(8x14)\t60;";
	
	private String sWin, sLouse, sStart, sNext;

	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case 0:
				break;
			}
			if (mode == 0) piper.startFluid(2);
			if (mode == 1) initApp(xMul, yMul);
			if (mode == 4)
			{
				mode = 0;
				initApp(xMul, yMul);
			}
		}
	};

	private class PipeView extends View
	{
		private String sTitle;
		private String[] sLine;
		private float titleWidth;
		private Paint paint;
		private float x;
		private float y;
		private float textHeight;

		public PipeView(Context context)
		{
			super(context);
			
			if(displayWidth < displayHeight) textHeight = displayWidth / 22;
			else textHeight = displayHeight / 22;

			paint = new Paint();
			paint.setColor(0xFFFFFFFF);
			paint.setTextSize(textHeight);
			paint.setStyle(Style.FILL);
			paint.setAntiAlias(true);
			paint.setFakeBoldText(true);
			paint.setShadowLayer(3, 0, 0, Color.BLACK);
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			if (canvas == null) return;

			try
			{
				if (backDrawable != null)
				{
					// canvas.save();
					// canvas.translate(xPos, yPos);
					backDrawable.draw(canvas);
					// canvas.translate(-xPos, -yPos);
					// canvas.restore();
				}

				if (piper != null && piper.ready)
				{
					for (PiperClass.Tile tile : piper.tiles)
					{
						if (tile != null && tile.visible)
						{
							x = ((xOffPos + tile.tilPosX) * zoomX) + xPos;
							y = ((yOffPos + tile.tilPosY) * zoomY) + yPos;
							
							if(x > -pipeSize * zoomX && x < displayWidth && y > - pipeSize * zoomY && y < displayHeight)
							{
								canvas.save();
								canvas.rotate(tile.tilRot,
										((xOffPos + tile.tilPosX) * zoomX) + xPos + tile.pipeBitmap.getWidth() * zoomX / 2,
										((yOffPos + tile.tilPosY) * zoomY) + yPos+ tile.pipeBitmap.getHeight() * zoomY / 2);
	
								canvas.translate(x, y);
	
								tile.pipeDrawable.draw(canvas);
	
								if (tile.fluidPercent > 0) tile.fluidDrawable.draw(canvas);
	
								canvas.restore();
							}
						}
					}
				}
				else Log.i(TAG, "piper is null or not ready!");

				if (mode == 0 || mode == 1)
				{
					if (mode == 0)
					{
						if(newGame) sTitle = sStart;
						else sTitle = sNext;
						titleWidth = paint.measureText(sTitle);
						canvas.drawText(sTitle, (displayWidth - titleWidth) / 2, displayHeight - textHeight, paint);
					}					
					if(newGame)
					{
						for(int i = 0;i < 5; i++)
						{
							sTitle = playerArr[i];
							sLine = sTitle.split("\t");
							canvas.drawText(sLine[0], pipeSize + xOffPos, displayHeight / 2 + pipeSize * 2 + i * (textHeight + textHeight / 2) + textHeight, paint);
							canvas.drawText(sLine[1], pipeSize * 4 + xOffPos, displayHeight / 2 + pipeSize * 2 + i * (textHeight + textHeight / 2) + textHeight, paint);
							canvas.drawText(sLine[2], pipeSize * 7 + xOffPos, displayHeight / 2 + pipeSize * 2 + i * (textHeight + textHeight / 2) + textHeight, paint);
						}
					}
				}
				else if (mode == 2)
				{
					sTitle = "" + piper.pipeCnt;
					titleWidth = paint.measureText(sTitle);
					canvas.drawText(sTitle, (displayWidth - titleWidth) / 2, displayHeight - textHeight, paint);
				}
				else if (mode == 3)
				{
					if (piper.getEndGame() == 1) sTitle = sWin;
					else if (piper.getEndGame() == 2) sTitle = sLouse;
					else sTitle = "" + piper.pipeCnt;
					titleWidth = paint.measureText(sTitle);
					canvas.drawText(sTitle, (displayWidth - titleWidth) / 2, displayHeight - textHeight, paint);
				}
			}
			catch (Exception ex)
			{
				Log.i(TAG, "error onDraw: " + ex);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();

		piperView = new PipeView(this);
		setContentView(piperView);

		sStart = getResources().getString(R.string.start);
		sNext = getResources().getString(R.string.next);
		sWin = getResources().getString(R.string.win);
		sLouse = getResources().getString(R.string.louse);

		if(displayWidth < displayHeight) pipeSize = displayWidth / pipeDiv;
		else pipeSize = displayHeight / pipeDiv;

		ui = new UI(this);

		swipeTimer = new Timer();
		swipeTimer.scheduleAtFixedRate(new swipeTask(), 0, SWIPE_INTERVAL);
	}

	// new game initialization
	private void initApp(float xMul, float yMul)
	{
		try
		{
			tableWidth = displayWidth * xMul;
			tableHeight = displayHeight * yMul;
			origWidth = tableWidth;
			origHeight = tableHeight;

			if (mode == 0)
			{
				onDoubleTap(0, 0);
				
				tableWidth = displayWidth;
				tableHeight = displayHeight;
				origWidth = tableWidth;
				origHeight = tableHeight;
				
				if(displayWidth < displayHeight) pipeSize = displayWidth / 9;
				else pipeSize = displayHeight / 9;
				
				piper = new PiperClass((int) tableWidth, (int) tableHeight, pipeSize, pipeSize, pipeColor, shadowColor, fluidColor);
				piper.newTitle();
				
				if(bFirst)
				{
					back = piper.getBackGround(displayWidth, displayHeight, getResources());
					backDrawable = new BitmapDrawable(back);
					backDrawable.setBounds(0, 0, displayWidth, displayHeight);
					bFirst = false;
				}
			}
			else
			{
				if(displayWidth < displayHeight) pipeSize = displayWidth / pipeDiv;
				else pipeSize = displayHeight / pipeDiv;
				
				piper.initClass((int) tableWidth, (int) tableHeight, pipeSize, pipeSize, pipeColor, shadowColor, fluidColor);
			}
			
			if (mode == 1)
			{
				mode = 2;
				while(!piper.newMaze(newGame));
				
				zoomX = 1;
				zoomY = 1;

				if (piper != null && piper.ready)
				{
					for (PiperClass.Tile tile : piper.tiles)
					{
						tile.pipeDrawable.setBounds(0, 0, (int) (tableWidth / piper.mTableWidth), (int) (tableHeight / piper.mTableHeight));
						tile.fluidDrawable.setBounds(0, 0, (int) (tableWidth / piper.mTableWidth), (int) (tableHeight / piper.mTableHeight));
					}
				}
			}

			// the margin is ten percent
			// offMarginX = (displayWidth / 10) * (displayWidth / displayWidth);
			// offMarginY = (displayHeight / 10) * (displayHeight / displayWidth);
			offMarginX = pipeSize;
			offMarginY = pipeSize;

			xPos = (displayWidth - tableWidth) / 2;
			yPos = (displayHeight - tableHeight) / 2;
			
			xOffPos = displayWidth;

			scrollIn = 1;
			tileSpeedX = -1;
		}
		catch (OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in new page!");
		}
		catch (Exception ex)
		{
			Log.e(TAG, "" + ex);
		}

		return;
	}
	
	@Override
	public void onBackPressed()
	{
		if(mode != 0)
		{
			newGame = true;
			
			mode = 4;
			
			updatePlayerList(piper.pipeCnt);
		
			// swipe out
			xOffPos = -1;
			scrollIn = 2;
			tileSpeedX = -1;	
		}
		else
		{
			this.finish();
		}
		
		//super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.piper, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.action_settings:
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public synchronized void onResume()
	{
		super.onResume();

		SharedPreferences settings = getSharedPreferences("org.landroo.piper_preferences", MODE_PRIVATE);

		// pipe size
		int i = settings.getInt("pipeSize", 8);
		pipeDiv = i;
		if(displayWidth < displayHeight) pipeSize = displayWidth / pipeDiv;
		else pipeSize = displayHeight / pipeDiv;

		// pipe color
		i = Integer.parseInt(settings.getString("pipeColor", "1"));
		switch (i)
		{
		case 1:
			pipeColor = 0xFFFFFFFF;
			shadowColor = 0xFF222222;
			break;
		case 2:
			pipeColor = 0xFFaa4422;
			shadowColor = 0xFF222222;
			break;
		case 3:
			pipeColor = 0xFFFFCC66;
			shadowColor = 0xFF222222;
			break;
		case 4:
			pipeColor = 0xFF000000;
			shadowColor = 0xFF888888;
			break;
		}

		// fluid color
		i = Integer.parseInt(settings.getString("fluidColor", "3"));
		switch (i)
		{
		case 1:
			fluidColor = Color.RED;
			break;
		case 2:
			fluidColor = Color.GREEN;
			break;
		case 3:
			fluidColor = Color.BLUE;
			break;
		case 4:
			fluidColor = Color.MAGENTA;
			break;
		case 5:
			fluidColor = Color.YELLOW;
			break;
		case 6:
			fluidColor = Color.CYAN;
			break;
		}

		float f = settings.getInt("tableSizeX", 100);
		xMul = f / 100;

		f = settings.getInt("tableSizeY", 100);
		yMul = f / 100;

		String s = "";
		s = settings.getString("player", "Player");
		s = s.replace(" ", "_");
		s = s.replace("?", "_");
		s = s.replace("=", "_");
		s = s.replace("&", "_");
		s = s.replace(";", "_");
		s = s.replace(":", "_");
		s = s.replace("/", "_");
		playerName = s;
		
		s = settings.getString("playerlist", playerList);
		playerArr = s.split(";");
		
		mode = 0;
		newGame = true;

		initApp(xMul, yMul);

		run = true;
	}

	@Override
	public synchronized void onPause()
	{
		super.onPause();
		
		saveState();
		
		run = false;
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return ui.tapEvent(event);
	}

	@Override
	public void onDown(float x, float y)
	{
		sX = x;
		sY = y;

		swipeVelocity = 0;

		piperView.postInvalidate();
	}

	@Override
	public void onUp(float x, float y)
	{
		checkOff();

		piperView.postInvalidate();
	}

	@Override
	public void onTap(float x, float y)
	{
		float bx = (x - xPos) * (origWidth / tableWidth);
		float by = (y - yPos) * (origHeight / tableHeight);

		// draw title
		if (mode == 0)
		{
			mode = 1;

			// swipe out
			xOffPos = -1;
			scrollIn = 2;
			tileSpeedX = -1;

			return;
		}

		// start a game
		if (mode == 2)
		{
			piper.rotateTiles();
			mode = 3;
			piper.startFluid(0);

			return;
		}

		// rotate the selected pipe
		if (mode == 3)
		{
			if (piper.getEndGame() == 0) piper.startRot(bx, by);
			else
			{
				mode = 1;
				
				newGame = false;
				if (piper.getEndGame() == 2)
				{
					newGame = true;
					
					mode = 4;
					
					updatePlayerList(piper.pipeCnt);
				}
				
				// swipe out
				xOffPos = -1;
				scrollIn = 2;
				tileSpeedX = -1;
			}
			
			return;
		}
		
		return;
	}

	@Override
	public void onHold(float x, float y)
	{
	}

	@Override
	public void onMove(float x, float y)
	{
		mX = x;
		mY = y;

		float dx = mX - sX;
		float dy = mY - sY;

		if ((tableWidth >= displayWidth) && (xPos + dx < displayWidth - (tableWidth + offMarginX) || xPos + dx > offMarginX)) dx = 0;
		if ((tableHeight >= displayHeight) && (yPos + dy < displayHeight - (tableHeight + offMarginY) || yPos + dy > offMarginY)) dy = 0;
		if ((tableWidth < displayWidth) && (xPos + dx > displayWidth - tableWidth || xPos + dx < 0)) dx = 0;
		if ((tableHeight < displayHeight) && (yPos + dy > displayHeight - tableHeight || yPos + dy < 0)) dy = 0;

		xPos += dx;
		yPos += dy;

		sX = mX;
		sY = mY;

		piperView.postInvalidate();

		return;
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		swipeDistX = x2 - x1;
		swipeDistY = y2 - y1;
		swipeSpeed = 1;
		swipeVelocity = velocity;

		piperView.postInvalidate();

		return;
	}

	@Override
	public void onDoubleTap(float x, float y)
	{
		swipeVelocity = 0;

		backSpeedX = 0;
		backSpeedY = 0;

		tableWidth = origWidth;
		tableHeight = origHeight;

		xPos = (displayWidth - tableWidth) / 2;
		yPos = (displayHeight - tableHeight) / 2;
		
		if (piper != null && piper.ready)
		{
			for (PiperClass.Tile tile : piper.tiles)
			{
				tile.pipeDrawable.setBounds(0, 0, (int) (tableWidth / piper.mTableWidth), (int) (tableHeight / piper.mTableHeight));
				tile.fluidDrawable.setBounds(0, 0, (int) (tableWidth / piper.mTableWidth), (int) (tableHeight / piper.mTableHeight));
			}
		}
		
		zoomX = 1;
		zoomY = 1;

		piperView.postInvalidate();

		return;
	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		int dist = (int) distance * 5;
		switch (mode)
		{
		case 1:
			zoomSize = dist;
			break;
		case 2:
			int diff = (int) (dist - zoomSize);
			float sizeNew = FloatMath.sqrt(tableWidth * tableWidth + tableHeight * tableHeight);
			float sizeDiff = 100 / (sizeNew / (sizeNew + diff));
			float newSizeX = tableWidth * sizeDiff / 100;
			float newSizeY = tableHeight * sizeDiff / 100;

			// zoom between min and max value
			if (newSizeX > origWidth / 4 && newSizeX < origWidth * 10)
			{
				// bitmapDrawable.setBounds(0, 0, (int)(newSizeX / displayWidth
				// * tileSize), (int)(newSizeY / displayHeight * tileSize));

				if (piper != null && piper.ready)
				{
					for (PiperClass.Tile tile : piper.tiles)
					{
						tile.pipeDrawable.setBounds(0, 0, (int) (newSizeX / piper.mTableWidth),
								(int) (newSizeY / piper.mTableHeight));
						tile.fluidDrawable.setBounds(0, 0, (int) (newSizeX / piper.mTableWidth),
								(int) (newSizeY / piper.mTableHeight));
					}
				}

				zoomSize = dist;

				float diffX = newSizeX - tableWidth;
				float diffY = newSizeY - tableHeight;
				float xPer = 100 / (tableWidth / (Math.abs(xPos) + mX)) / 100;
				float yPer = 100 / (tableHeight / (Math.abs(yPos) + mY)) / 100;

				xPos -= diffX * xPer;
				yPos -= diffY * yPer;

				tableWidth = newSizeX;
				tableHeight = newSizeY;

				if (tableWidth > displayWidth || tableHeight > displayHeight)
				{
					if (xPos > 0) xPos = 0;
					if (yPos > 0) yPos = 0;

					if (xPos + tableWidth < displayWidth) xPos = displayWidth - tableWidth;
					if (yPos + tableHeight < displayHeight) yPos = displayHeight - tableHeight;
				}
				else
				{
					if (xPos <= 0) xPos = 0;
					if (yPos <= 0) yPos = 0;

					if (xPos + tableWidth > displayWidth) xPos = displayWidth - tableWidth;
					if (yPos + tableHeight > displayHeight) yPos = displayHeight - tableHeight;
				}
				
				zoomX = tableWidth / origWidth;
				zoomY = tableHeight / origHeight;

				// Log.i(TAG, "" + xPos + " " + yPos);
			}
			break;
		case 3:
			zoomSize = 0;
			break;
		}

		piperView.postInvalidate();

		return;
	}

	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
	}

	@Override
	public void onFingerChange()
	{
	}

	// swipe timer class
	class swipeTask extends TimerTask
	{
		// draw the moving graphics
		public void run()
		{
			boolean draw = false;
			if (run && swipeVelocity > 0)
			{
				float dist = FloatMath.sqrt(swipeDistY * swipeDistY + swipeDistX * swipeDistX);
				float x = xPos - (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				float y = yPos - (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				if ((tableWidth >= displayWidth) && (x < displayWidth - (tableWidth + offMarginX) || x > offMarginX)
						|| ((tableWidth < displayWidth) && (x > displayWidth - tableWidth || x < 0)))
				{
					swipeDistX *= -1;
					swipeSpeed = swipeVelocity;
					// swipeSpeed += .5;
				}

				if ((tableHeight >= displayHeight) && (y < displayHeight - (tableHeight + offMarginY) || y > offMarginY)
						|| ((tableHeight < displayHeight) && (y > displayHeight - tableHeight || y < 0)))
				{
					swipeDistY *= -1;
					swipeSpeed = swipeVelocity;
					// swipeSpeed += .5;
				}

				xPos -= (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				yPos -= (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				swipeVelocity -= swipeSpeed;
				swipeSpeed += .0001;

				draw = true;

				if (swipeVelocity <= 0) checkOff();
			}

			if (backSpeedX != 0)
			{
				if ((backSpeedX < 0 && xPos <= 0.1f) || (backSpeedX > 0 && xPos + 0.1f >= displayWidth - tableWidth)) backSpeedX = 0;
				else if (backSpeedX < 0) xPos -= xPos / 20;
				else xPos += (displayWidth - (tableWidth + xPos)) / 20;

				draw = true;
			}

			if (backSpeedY != 0)
			{
				if ((backSpeedY < 0 && yPos <= 0.1f) || (backSpeedY > 0 && yPos + 0.1f >= displayHeight - tableHeight)) backSpeedY = 0;
				else if (backSpeedY < 0) yPos -= yPos / 20;
				else yPos += (displayHeight - (tableHeight + yPos)) / 20;

				draw = true;
			}

			if (run && piper != null && piper.ready)
			{
				boolean fluid = piper.updateFluid(fluidColor);
				if (fluid) draw = true;
			}

			if (run && piper != null && piper.ready)
			{
				boolean rot = piper.rotTiles();
				if (rot) draw = true;
			}

			if (run && piper != null && piper.ready)
			{
				boolean scroll = scrollEffect();
				if (scroll) draw = true;
			}

			if (draw) piperView.postInvalidate();

			return;
		}
	}

	// check the offset of the play table
	private void checkOff()
	{
		if (tableWidth >= displayWidth)
		{
			if (xPos > 0 && xPos <= offMarginX) backSpeedX = -1;
			else if (xPos < tableWidth - offMarginX && xPos <= tableWidth) backSpeedX = 1;
		}
		if (tableHeight >= displayHeight)
		{
			if (yPos > 0 && yPos <= offMarginY) backSpeedY = -1;
			else if (yPos < tableHeight - offMarginY && yPos <= tableHeight) backSpeedY = 1;
		}
	}

	// 0 stop 1 in 2 out
	private boolean scrollEffect()
	{
		boolean draw = false;
		if (piper != null && piper.ready)
		{
			if (scrollIn == 1)
			{
				if (tileSpeedX != 0)
				{
					if ((tileSpeedX < 0 && xOffPos <= 0.1f)
							|| (tileSpeedX > 0 && xOffPos + 0.1f >= displayWidth - tableWidth)) tileSpeedX = 0;
					else if (tileSpeedX < 0) xOffPos -= xOffPos / 20;
					else xOffPos += (displayWidth - (tableWidth + xOffPos)) / 20;

					if (tileSpeedX == 0) handler.sendEmptyMessage(0);

					draw = true;
				}

				if (tileSpeedY != 0)
				{
					if ((tileSpeedY < 0 && yOffPos <= 0.1f)
							|| (tileSpeedY > 0 && yOffPos + 0.1f >= displayHeight - tableHeight)) tileSpeedY = 0;
					else if (tileSpeedY < 0) yOffPos -= yOffPos / 20;
					else yOffPos += (displayHeight - (tableHeight + yOffPos)) / 20;

					if (tileSpeedX == 0) handler.sendEmptyMessage(0);

					draw = true;
				}
			}
			if (scrollIn == 2)
			{
				if (tileSpeedX != 0)
				{
					if ((tileSpeedX > 0 && xOffPos >= 0.1f) || (tileSpeedX < 0 && xOffPos - 0.1f <= -tableWidth)) tileSpeedX = 0;
					else if (tileSpeedX < 0) xOffPos += xOffPos / 20;
					else xOffPos -= (displayWidth - (tableWidth + xOffPos)) / 20;

					if (tileSpeedX == 0) handler.sendEmptyMessage(0);

					draw = true;
				}

				if (tileSpeedY != 0)
				{
					if ((tileSpeedY < 0 && yOffPos >= 0.1f) || (tileSpeedY > 0 && yOffPos + 0.1f <= displayHeight - tableHeight)) tileSpeedY = 0;
					else if (tileSpeedY < 0) yOffPos += yOffPos / 20;
					else yOffPos -= (displayHeight - (tableHeight + yOffPos)) / 20;

					if (tileSpeedX == 0) handler.sendEmptyMessage(0);

					draw = true;
				}
			}
		}
		return draw;
	}

	private void saveState()
	{
		SharedPreferences settings = getSharedPreferences("org.landroo.piper_preferences", MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		editor.putString("playerlist", playerList);
		editor.commit();

		return;
	}
	
	private void updatePlayerList(int iNo)
	{
		String sTitle;
		String[] sLine;
		int n;
		for(int i = 0;i < 5; i++)
		{
			sTitle = playerArr[i];
			sLine = sTitle.split("\t");
			try
			{
				n = Integer.parseInt(sLine[2]);
				if(iNo >= n)
				{
					for(int j = 4; j > i; j--) playerArr[j] = playerArr[j - 1];
					playerArr[i] = playerName + "\t" + "(" + piper.mTableWidth + "x" + piper.mTableHeight + ")\t" + iNo;
					break;
				}
			}
			catch(Exception ex)
			{
				Log.i(TAG, "" + ex);
			}
		}

		playerList = "";
		for(int i = 0;i < 5; i++)
		{
			sTitle = playerArr[i];
			playerList += sTitle + ";"; 
		}		
	}
}

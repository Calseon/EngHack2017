package enghack17.myowninstrument;
import java.io.File;
import java.util.Date;

public class PulseDetect {
	enum StateMachine {MID, RISE, FALL}
	final long minMillisecondDelay = 200;
	final double pulseThreshold = 0.3;

	private Date timeOfLastPulse;
	private double[] previousPoints;		// Index 0 is the most recent, Largest index is the oldest
	public StateMachine programState;
	private StateMachine previousState;
	private double restingThreshold = 0.04;

	public PulseDetect()
	{
		programState = StateMachine.MID;
		previousPoints = new double[5];
		for(int i = 0; i < previousPoints.length; i++) {
			previousPoints[i] = 0;
		}
		timeOfLastPulse = new Date();
	}

	/*public boolean pulseDetected()
	{
		boolean passedPulseThreshold = false;
		switch (programState){
			case REST:
			{
				boolean rising = true;
				boolean falling = true;
				for(int i = 0; i < previousPoints.length-1; i++)
				{
					rising = rising && (previousPoints[i] > previousPoints[i+1]);
					falling = falling && (previousPoints[i] < previousPoints[i+1]);
					passedPulseThreshold = passedPulseThreshold || ((Math.abs(previousPoints[i])> pulseThreshold));
				}
				if(rising) {
					programState = StateMachine.RISE;
					previousState = StateMachine.REST;
				}
				else if (falling)
				{
					programState = StateMachine.FALL;
					previousState = StateMachine.REST;
				}
				return false;
			}
			case RISE:
			{
				boolean rest = true;
				boolean falling = true;

				double sum = 0;
				for(int i = 0; i < previousPoints.length-1; i++)
				{
					sum += previousPoints[i];
					falling = falling && (previousPoints[i] < previousPoints[i+1]);
					passedPulseThreshold = passedPulseThreshold || ((Math.abs(previousPoints[i])> pulseThreshold));
				}
				if (falling && passedPulseThreshold)
				{
					programState = StateMachine.FALL;
					previousState = StateMachine.RISE;
					return true;
				}

				for(int i = 0; i < previousPoints.length-1; i++)
				{
					rest = rest && (Math.abs(previousPoints[i] - previousPoints[i+1]) < restingThreshold);
				}
				if (rest)
				{
					programState = StateMachine.REST;
					previousState = StateMachine.RISE;
					return false;
				}
				return false;
			}
			case FALL:
			{
				boolean rest = true;
				boolean rising = true;

				double sum = 0;
				for(int i = 0; i < previousPoints.length-1; i++)
				{
					sum += previousPoints[i];
					rising = rising && (previousPoints[i] < previousPoints[i+1]);
					passedPulseThreshold = passedPulseThreshold || ((Math.abs(previousPoints[i])> pulseThreshold));
				}
				if (rising && passedPulseThreshold)
				{
					programState = StateMachine.RISE;
					previousState = StateMachine.FALL;
					return true;
				}

				for(int i = 0; i < previousPoints.length-1; i++)
				{
					rest = rest && (Math.abs(previousPoints[i] - previousPoints[i+1]) < restingThreshold);
				}
				if (rest)
				{
					programState = StateMachine.REST;
					previousState = StateMachine.FALL;
					return false;
				}
				return false;
			}
		}
		return false;
	}
*/

	public boolean pulseDetected()
	{
		int currentPoint = 0;
		switch (programState)
		{
			case MID:
			{
				if (Math.abs(previousPoints[currentPoint]) > pulseThreshold)
				{
					programState = StateMachine.RISE;
					if (DelayPassed())
						return true;
				}
				if (Math.abs(previousPoints[currentPoint]) <  (pulseThreshold * -1))
				{
					programState = StateMachine.FALL;
					if (DelayPassed())
						return true;
				}
			}
			case RISE:
			{
				if (Math.abs(previousPoints[currentPoint]) < pulseThreshold)
				{
					programState = StateMachine.MID;
				}
			}
			case FALL:
			{
				if (Math.abs(previousPoints[currentPoint]) > (pulseThreshold * -1))
				{
					programState = StateMachine.MID;
				}
			}
		}
		return false;
	}
	public void AddNewPoint(double newPoint)
	{
		// Shift the points
		for(int i = previousPoints.length - 1; i>0; i--)
		{
			previousPoints[i] = previousPoints[i-1];
		}
		previousPoints[0] = newPoint;
	}

	private boolean DelayPassed()
	{
		Date currentTime = new Date();
		long difference = currentTime.getTime() - timeOfLastPulse.getTime();
		if (difference > minMillisecondDelay)
		{
			timeOfLastPulse = currentTime;
			return true;
		}
		else
			return false;
	}
}

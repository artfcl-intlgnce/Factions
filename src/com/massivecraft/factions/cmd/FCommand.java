package com.massivecraft.factions.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.FactionFlag;
import com.massivecraft.factions.struct.FactionPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.MCommand;


public abstract class FCommand extends MCommand<P>
{
	public boolean disableOnLock;
	
	public FPlayer fme;
	public Faction myFaction;
	public boolean senderMustBeMember;
	public boolean senderMustBeOfficer;
	public boolean senderMustBeLeader;
	
	public boolean isMoneyCommand;
	
	public FCommand()
	{
		super(P.p);
		
		// Due to safety reasons it defaults to disable on lock.
		disableOnLock = true;
		
		// The money commands must be disabled if money should not be used.
		isMoneyCommand = false;
		
		senderMustBeMember = false;
		senderMustBeOfficer = false;
		senderMustBeLeader = false;
	}
	
	@Override
	public void execute(CommandSender sender, List<String> args, List<MCommand<?>> commandChain)
	{
		if (sender instanceof Player)
		{
			this.fme = FPlayers.i.get((Player)sender);
			this.myFaction = this.fme.getFaction();
		}
		else
		{
			this.fme = null;
			this.myFaction = null;
		}
		super.execute(sender, args, commandChain);
	}
	
	@Override
	public boolean isEnabled()
	{
		if (p.getLocked() && this.disableOnLock)
		{
			msg("<b>Factions was locked by an admin. Please try again later.");
			return false;
		}
		
		if (this.isMoneyCommand && ! Conf.econEnabled)
		{
			msg("<b>Faction economy features are disabled on this server.");
			return false;
		}
		
		if (this.isMoneyCommand && ! Conf.bankEnabled)
		{
			msg("<b>The faction bank system is disabled on this server.");
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean validSenderType(CommandSender sender, boolean informSenderIfNot)
	{
		boolean superValid = super.validSenderType(sender, informSenderIfNot);
		if ( ! superValid) return false;
		
		if ( ! (this.senderMustBeMember || this.senderMustBeOfficer || this.senderMustBeLeader)) return true;
		
		if ( ! (sender instanceof Player)) return false;
		
		FPlayer fplayer = FPlayers.i.get((Player)sender);
		
		if ( ! fplayer.hasFaction())
		{
			sender.sendMessage(p.txt.parse("<b>You are not member of any faction."));
			return false;
		}
		
		if (this.senderMustBeOfficer && ! fplayer.getRole().isAtLeast(Rel.OFFICER))
		{
			sender.sendMessage(p.txt.parse("<b>Only faction moderators can %s.", this.getHelpShort()));
			return false;
		}
		
		if (this.senderMustBeLeader && ! fplayer.getRole().isAtLeast(Rel.LEADER))
		{
			sender.sendMessage(p.txt.parse("<b>Only faction admins can %s.", this.getHelpShort()));
			return false;
		}
			
		return true;
	}
	
	// -------------------------------------------- //
	// Assertions
	// -------------------------------------------- //

	public boolean assertHasFaction()
	{
		if (me == null) return true;
		
		if ( ! fme.hasFaction())
		{
			sendMessage("You are not member of any faction.");
			return false;
		}
		return true;
	}

	public boolean assertMinRole(Rel role)
	{
		if (me == null) return true;
		
		if (fme.getRole().value < role.value)
		{
			msg("<b>You <h>must be "+role+"<b> to "+this.getHelpShort()+".");
			return false;
		}
		return true;
	}
	
	// -------------------------------------------- //
	// Argument Readers
	// -------------------------------------------- //
	
	// FPLAYER ======================
	public FPlayer strAsFPlayer(String name, FPlayer def, boolean msg)
	{
		FPlayer ret = def;
		
		if (name != null)
		{
			FPlayer fplayer = FPlayers.i.get(name);
			if (fplayer != null)
			{
				ret = fplayer;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>No player \"<p>%s<b>\" could not be found.", name);			
		}
		
		return ret;
	}
	public FPlayer argAsFPlayer(int idx, FPlayer def, boolean msg)
	{
		return this.strAsFPlayer(this.argAsString(idx), def, msg);
	}
	public FPlayer argAsFPlayer(int idx, FPlayer def)
	{
		return this.argAsFPlayer(idx, def, true);
	}
	public FPlayer argAsFPlayer(int idx)
	{
		return this.argAsFPlayer(idx, null);
	}
	
	// BEST FPLAYER MATCH ======================
	public FPlayer strAsBestFPlayerMatch(String name, FPlayer def, boolean msg)
	{
		FPlayer ret = def;
		
		if (name != null)
		{
			FPlayer fplayer = FPlayers.i.getBestIdMatch(name);
			if (fplayer != null)
			{
				ret = fplayer;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>No player match found for \"<p>%s<b>\".", name);
		}
		
		return ret;
	}
	public FPlayer argAsBestFPlayerMatch(int idx, FPlayer def, boolean msg)
	{
		return this.strAsBestFPlayerMatch(this.argAsString(idx), def, msg);
	}
	public FPlayer argAsBestFPlayerMatch(int idx, FPlayer def)
	{
		return this.argAsBestFPlayerMatch(idx, def, true);
	}
	public FPlayer argAsBestFPlayerMatch(int idx)
	{
		return this.argAsBestFPlayerMatch(idx, null);
	}
	
	// FACTION ======================
	public Faction strAsFaction(String name, Faction def, boolean msg)
	{
		Faction ret = def;
		
		if (name != null)
		{
			Faction faction = null;
			
			// First we try an exact match
			if (faction == null)
			{
				faction = Factions.i.getByTag(name);
			}
			
			// Next we match faction tags
			if (faction == null)
			{
				faction = Factions.i.getBestTagMatch(name);
			}
				
			// Next we match player names
			if (faction == null)
			{
				FPlayer fplayer = FPlayers.i.getBestIdMatch(name);
				if (fplayer != null)
				{
					faction = fplayer.getFaction();
				}
			}
			
			if (faction != null)
			{
				ret = faction;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The faction or player \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public Faction argAsFaction(int idx, Faction def, boolean msg)
	{
		return this.strAsFaction(this.argAsString(idx), def, msg);
	}
	public Faction argAsFaction(int idx, Faction def)
	{
		return this.argAsFaction(idx, def, true);
	}
	public Faction argAsFaction(int idx)
	{
		return this.argAsFaction(idx, null);
	}
	
	// FACTION FLAG ======================
	public FactionFlag strAsFactionFlag(String name, FactionFlag def, boolean msg)
	{
		FactionFlag ret = def;
		
		if (name != null)
		{
			FactionFlag flag = FactionFlag.parse(name);
			if (flag != null)
			{
				ret = flag;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The faction-flag \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public FactionFlag argAsFactionFlag(int idx, FactionFlag def, boolean msg)
	{
		return this.strAsFactionFlag(this.argAsString(idx), def, msg);
	}
	public FactionFlag argAsFactionFlag(int idx, FactionFlag def)
	{
		return this.argAsFactionFlag(idx, def, true);
	}
	public FactionFlag argAsFactionFlag(int idx)
	{
		return this.argAsFactionFlag(idx, null);
	}
	
	// FACTION PERM ======================
	public FactionPerm strAsFactionPerm(String name, FactionPerm def, boolean msg)
	{
		FactionPerm ret = def;
		
		if (name != null)
		{
			FactionPerm perm = FactionPerm.parse(name);
			if (perm != null)
			{
				ret = perm;
			}
		}
		
		if (msg && ret == null)
		{
			this.msg("<b>The faction-perm \"<p>%s<b>\" could not be found.", name);
		}
		
		return ret;
	}
	public FactionPerm argAsFactionPerm(int idx, FactionPerm def, boolean msg)
	{
		return this.strAsFactionPerm(this.argAsString(idx), def, msg);
	}
	public FactionPerm argAsFactionPerm(int idx, FactionPerm def)
	{
		return this.argAsFactionPerm(idx, def, true);
	}
	public FactionPerm argAsFactionPerm(int idx)
	{
		return this.argAsFactionPerm(idx, null);
	}
	
	
	// -------------------------------------------- //
	// Commonly used logic
	// -------------------------------------------- //
	
	public boolean canIAdministerYou(FPlayer i, FPlayer you)
	{
		if ( ! i.getFaction().equals(you.getFaction()))
		{
			i.sendMessage(p.txt.parse("%s <b>is not in the same faction as you.",you.describeTo(i, true)));
			return false;
		}
		
		if (i.getRole().value > you.getRole().value || i.getRole().equals(Rel.LEADER) )
		{
			return true;
		}
		
		if (you.getRole().equals(Rel.LEADER))
		{
			i.sendMessage(p.txt.parse("<b>Only the faction admin can do that."));
		}
		else if (i.getRole().equals(Rel.OFFICER))
		{
			if ( i == you )
			{
				return true; //Moderators can control themselves
			}
			else
			{
				i.sendMessage(p.txt.parse("<b>Moderators can't control each other..."));
			}
		}
		else
		{
			i.sendMessage(p.txt.parse("<b>You must be a faction moderator to do that."));
		}
		
		return false;
	}
	
	// if economy is enabled and they're not on the bypass list, make 'em pay; returns true unless person can't afford the cost
	public boolean payForCommand(double cost, String toDoThis, String forDoingThis)
	{
		if ( ! Econ.shouldBeUsed() || this.fme == null || cost == 0.0 || fme.isAdminBypassing()) return true;

		if(Conf.bankFactionPaysLandCosts && fme.hasFaction())
		{
			if ( ! Econ.modifyMoney(myFaction, -cost, toDoThis, forDoingThis)) return false;
		}
		else
		{
			if ( ! Econ.modifyMoney(fme, -cost, toDoThis, forDoingThis)) return false;
		}
		return true;
		/*
		
		
		
		// pay up
		if (cost > 0.0)
		{
			String costString = Econ.moneyString(cost);
			if(Conf.bankFactionPaysCosts && fme.hasFaction() )
			{
				if( ! faction.getAccount().subtract(cost))
				{
					sendMessage("It costs "+costString+" to "+desc+", which your faction can't currently afford.");
					return false;
				}
				else
				{
					sendMessage(faction.getTag()+" has paid "+costString+" to "+desc+".");
				}
					
			}
			else
			{
				if ( ! Econ.deductMoney(fme.getName(), cost))
				{
					sendMessage("It costs "+costString+" to "+desc+", which you can't currently afford.");
					return false;
				}
				sendMessage("You have paid "+costString+" to "+desc+".");
			}
		}
		// wait... we pay you to use this command?
		else
		{
			String costString = Econ.moneyString(-cost);
			
			if(Conf.bankFactionPaysCosts && fme.hasFaction() )
			{
				faction.getAccount().add(-cost);
				sendMessage(faction.getTag()+" has been paid "+costString+" to "+desc+".");
			}
			else
			{
				Econ.addMoney(fme.getName(), -cost);
			}
			
			
			sendMessage("You have been paid "+costString+" to "+desc+".");
		}
		return true;*/
	}
}

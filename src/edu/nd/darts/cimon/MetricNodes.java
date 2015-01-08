/*
 * Copyright (C) 2013 Chris Miller
 *
 * This file is part of CIMON.
 * 
 * CIMON is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * CIMON is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with CIMON.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package edu.nd.darts.cimon;

/**
 * Maintains a static list of all currently instantiated metric management nodes.
 * When a call needs to be made to a management node ({@link CurrentNode}) for a
 * particular metric, this list should be referenced first to determine if an
 * instance of that node already exist.  If not, a new node may be instantiated and
 * added to this list. This is to ensure there is only one active management node
 * for each metric.
 *  
 * @author darts
 * 
 * @deprecated
 *
 */
public final class MetricNodes {

	private static final MetricNodes INSTANCE = new MetricNodes();
	/** 
	 * List of all currently instantiated metric management nodes.
	 * Nodes are indexed based on integer reference defined in {@link Metrics}.
	 * Value will be null if metric node not currently active. 
	 */
//	private static final CurrentNode<?>[] nodes = new CurrentNode[Metrics.MAX_METRIC];
	
	private MetricNodes() {
		if (INSTANCE != null) {
			throw new IllegalStateException("MetricNodes already instantiated");
		}
	}
	
	/** Return singleton instance of MetricNodes. */
	public static MetricNodes getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Get management node ({@link CurrentNode}) for metric.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics})
	 * @return    management node for metric
	 */
/*	public <T> CurrentNode<T> getNode(int metric) {
		return (CurrentNode<T>) nodes[metric];
	}*/
	
	/**
	 * Set management node ({@link CurrentNode}) for metric.
	 * Should only be used if node doesn't currently exist for metric.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics})
	 * @param node    new metric management node
	 */
/*	public <T> void setNode(int metric, CurrentNode<T> node) {
		nodes[metric] = node;
	}*/
}

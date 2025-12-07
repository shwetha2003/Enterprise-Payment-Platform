import React, { useState, useEffect } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  CardHeader,
  LinearProgress,
  Chip,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from '@mui/material';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import PeopleIcon from '@mui/icons-material/People';
import ScheduleIcon from '@mui/icons-material/Schedule';
import { analyticsService } from '../services/api';

interface MetricCardProps {
  title: string;
  value: string | number;
  change: number;
  icon: React.ReactNode;
  subtitle?: string;
}

const MetricCard: React.FC<MetricCardProps> = ({ title, value, change, icon, subtitle }) => (
  <Card>
    <CardContent>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography color="textSecondary" gutterBottom variant="overline">
            {title}
          </Typography>
          <Typography variant="h4">{value}</Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
            {change >= 0 ? (
              <TrendingUpIcon sx={{ color: 'success.main', mr: 0.5 }} />
            ) : (
              <TrendingDownIcon sx={{ color: 'error.main', mr: 0.5 }} />
            )}
            <Typography
              variant="body2"
              sx={{ color: change >= 0 ? 'success.main' : 'error.main' }}
            >
              {change >= 0 ? '+' : ''}{change}%
            </Typography>
            <Typography variant="body2" color="textSecondary" sx={{ ml: 1 }}>
              vs last period
            </Typography>
          </Box>
          {subtitle && (
            <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        <Box sx={{ backgroundColor: 'primary.light', borderRadius: 2, p: 1 }}>
          {icon}
        </Box>
      </Box>
    </CardContent>
  </Card>
);

const Analytics: React.FC = () => {
  const [timeRange, setTimeRange] = useState('month');
  const [startDate, setStartDate] = useState<Date | null>(new Date());
  const [endDate, setEndDate] = useState<Date | null>(new Date());
  const [metrics, setMetrics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [arAgingData, setArAgingData] = useState<any[]>([]);
  const [paymentTrends, setPaymentTrends] = useState<any[]>([]);
  const [customerSegments, setCustomerSegments] = useState<any[]>([]);

  useEffect(() => {
    loadAnalyticsData();
  }, [timeRange]);

  const loadAnalyticsData = async () => {
    setLoading(true);
    try {
      const [
        mrrData,
        churnData,
        arAging,
        trends,
        segments,
        keyMetrics
      ] = await Promise.all([
        analyticsService.getMRR(timeRange),
        analyticsService.getChurnRate(timeRange),
        analyticsService.getARAging(),
        analyticsService.getPaymentTrends(),
        analyticsService.getCustomerSegmentation(),
        analyticsService.getKeyMetrics()
      ]);

      setMetrics(keyMetrics);
      setArAgingData(arAging);
      setPaymentTrends(trends);
      setCustomerSegments(segments);
    } catch (error) {
      console.error('Failed to load analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LinearProgress />;
  }

  // Mock data for charts
  const revenueData = [
    { month: 'Jan', revenue: 125000, forecast: 130000 },
    { month: 'Feb', revenue: 132000, forecast: 135000 },
    { month: 'Mar', revenue: 141000, forecast: 140000 },
    { month: 'Apr', revenue: 148000, forecast: 145000 },
    { month: 'May', revenue: 152000, forecast: 150000 },
    { month: 'Jun', revenue: 158000, forecast: 155000 },
  ];

  const agingColors = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

  const segmentColors = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Business Analytics
        </Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Time Range</InputLabel>
            <Select
              value={timeRange}
              label="Time Range"
              onChange={(e) => setTimeRange(e.target.value)}
            >
              <MenuItem value="week">Last Week</MenuItem>
              <MenuItem value="month">Last Month</MenuItem>
              <MenuItem value="quarter">Last Quarter</MenuItem>
              <MenuItem value="year">Last Year</MenuItem>
            </Select>
          </FormControl>
          <LocalizationProvider dateAdapter={AdapterDateFns}>
            <DatePicker
              label="Start Date"
              value={startDate}
              onChange={setStartDate}
              slotProps={{ textField: { size: 'small' } }}
            />
            <DatePicker
              label="End Date"
              value={endDate}
              onChange={setEndDate}
              slotProps={{ textField: { size: 'small' } }}
            />
          </LocalizationProvider>
          <Button variant="contained" onClick={loadAnalyticsData}>
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Key Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Monthly Recurring Revenue"
            value={`$${metrics?.mrr?.toLocaleString() || '0'}`}
            change={4.5}
            icon={<AttachMoneyIcon sx={{ color: 'white' }} />}
            subtitle="Growth from last month"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Customer Churn Rate"
            value={`${metrics?.churnRate?.toFixed(2) || '0'}%`}
            change={-1.2}
            icon={<PeopleIcon sx={{ color: 'white' }} />}
            subtitle="Lower is better"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Days Sales Outstanding"
            value={`${metrics?.dso?.toFixed(1) || '0'} days`}
            change={-2.1}
            icon={<ScheduleIcon sx={{ color: 'white' }} />}
            subtitle="Industry avg: 45 days"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Collection Effectiveness"
            value={`${metrics?.collectionEffectiveness?.toFixed(1) || '0'}%`}
            change={3.2}
            icon={<TrendingUpIcon sx={{ color: 'white' }} />}
            subtitle="Best in class: >85%"
          />
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Revenue Trend */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Revenue Trend & Forecast
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={revenueData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="revenue"
                  stroke="#1976d2"
                  strokeWidth={2}
                  name="Actual Revenue"
                />
                <Line
                  type="monotone"
                  dataKey="forecast"
                  stroke="#dc004e"
                  strokeWidth={2}
                  strokeDasharray="5 5"
                  name="Forecast"
                />
              </LineChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* AR Aging */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              AR Aging Summary
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={arAgingData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {arAgingData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={agingColors[index % agingColors.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            <Box sx={{ mt: 2 }}>
              {arAgingData.map((item, index) => (
                <Chip
                  key={index}
                  label={`${item.name}: $${item.value?.toLocaleString()}`}
                  sx={{
                    backgroundColor: agingColors[index],
                    color: 'white',
                    mr: 1,
                    mb: 1,
                  }}
                  size="small"
                />
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Payment Trends */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Payment Timing Distribution
            </Typography>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={paymentTrends}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="onTime" fill="#00C49F" name="On Time" />
                <Bar dataKey="late" fill="#FF8042" name="Late" />
                <Bar dataKey="veryLate" fill="#FF0000" name="Very Late" />
              </BarChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Customer Segmentation */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Customer Segments
            </Typography>
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie
                  data={customerSegments}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, value }) => `${name}: ${value}`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {customerSegments.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={segmentColors[index % segmentColors.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            <Box sx={{ mt: 2 }}>
              {customerSegments.map((segment, index) => (
                <Box
                  key={index}
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mb: 1,
                    p: 1,
                    backgroundColor: 'action.hover',
                    borderRadius: 1,
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Box
                      sx={{
                        width: 12,
                        height: 12,
                        backgroundColor: segmentColors[index],
                        borderRadius: '50%',
                        mr: 1,
                      }}
                    />
                    <Typography variant="body2">{segment.name}</Typography>
                  </Box>
                  <Typography variant="body2" fontWeight="medium">
                    {segment.value} customers
                  </Typography>
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Risk Analysis */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              High Risk Accounts
            </Typography>
            <Grid container spacing={2}>
              {metrics?.highRiskAccounts?.slice(0, 4).map((account: any, index: number) => (
                <Grid item xs={12} sm={6} md={3} key={index}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="subtitle2" gutterBottom>
                        {account.companyName}
                      </Typography>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" color="textSecondary">
                          Risk Score:
                        </Typography>
                        <Chip
                          label={`${account.riskScore}%`}
                          size="small"
                          color={account.riskScore > 70 ? 'error' : account.riskScore > 40 ? 'warning' : 'success'}
                        />
                      </Box>
                      <Typography variant="caption" color="textSecondary">
                        Outstanding: ${account.outstandingBalance?.toLocaleString()}
                      </Typography>
                      <Box sx={{ mt: 1 }}>
                        <LinearProgress
                          variant="determinate"
                          value={account.riskScore}
                          color={account.riskScore > 70 ? 'error' : account.riskScore > 40 ? 'warning' : 'success'}
                        />
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Analytics;
